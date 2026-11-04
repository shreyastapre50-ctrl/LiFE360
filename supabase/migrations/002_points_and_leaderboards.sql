-- LiFE360 points, global leaderboard, and private college/community groups.
-- Apply only after reviewing in Supabase SQL Editor. Points are server-awarded;
-- do not expose direct INSERT permissions to the mobile client.

create table if not exists public.community_groups (
    id uuid primary key default gen_random_uuid(),
    name text not null check (char_length(trim(name)) between 2 and 80),
    invite_code text not null unique,
    created_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now()
);

create table if not exists public.community_group_members (
    group_id uuid not null references public.community_groups(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    joined_at timestamptz not null default now(),
    primary key (group_id, user_id)
);

create table if not exists public.sustainability_point_events (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    points integer not null check (points > 0 and points <= 10000),
    category text not null check (category in ('transport', 'food', 'energy', 'waste', 'water', 'challenge')),
    reason text not null check (char_length(trim(reason)) between 3 and 160),
    source_type text not null check (source_type in ('journey', 'meal', 'energy_reading', 'challenge', 'manual_adjustment')),
    source_id uuid,
    awarded_at timestamptz not null default now(),
    -- Prevent duplicate awards for the same source/action.
    unique (user_id, source_type, source_id, category)
);

create index if not exists idx_point_events_user_time
    on public.sustainability_point_events(user_id, awarded_at desc);
create index if not exists idx_group_members_user
    on public.community_group_members(user_id);
create index if not exists idx_group_members_group
    on public.community_group_members(group_id);

alter table public.community_groups enable row level security;
alter table public.community_group_members enable row level security;
alter table public.sustainability_point_events enable row level security;

-- Users can see groups they belong to, and create/join actions should be
-- performed through validated backend functions in a later security step.
create policy "members can view their groups"
    on public.community_groups for select
    using (exists (
        select 1 from public.community_group_members m
        where m.group_id = community_groups.id and m.user_id = auth.uid()
    ));

create policy "users can view group membership for their groups"
    on public.community_group_members for select
    using (exists (
        select 1 from public.community_group_members mine
        where mine.group_id = community_group_members.group_id
          and mine.user_id = auth.uid()
    ));

create policy "users can view their own point events"
    on public.sustainability_point_events for select
    using (user_id = auth.uid());

-- No INSERT/UPDATE/DELETE policies for point events: clients must not be able
-- to award themselves points. A trusted Edge Function will award points after
-- validating journeys, meals, energy readings, and challenge completion.

create or replace function public.life360_global_leaderboard(
    p_period_start timestamptz default null
)
returns table (
    user_id uuid,
    display_name text,
    total_points bigint,
    rank bigint
)
language sql
stable
security invoker
set search_path = public
as $$
    with totals as (
        select e.user_id, sum(e.points)::bigint as total_points
        from public.sustainability_point_events e
        where p_period_start is null or e.awarded_at >= p_period_start
        group by e.user_id
    ), ranked as (
        select t.user_id, coalesce(nullif(trim(p.display_name), ''), 'LiFE360 User') as display_name,
               t.total_points,
               dense_rank() over (order by t.total_points desc) as rank
        from totals t
        left join public.profiles p on p.id = t.user_id
    )
    select r.user_id, r.display_name, r.total_points, r.rank
    from ranked r
    order by r.rank, r.display_name
    limit 100;
$$;

create or replace function public.life360_group_leaderboard(
    p_group_id uuid,
    p_period_start timestamptz default null
)
returns table (
    user_id uuid,
    display_name text,
    total_points bigint,
    rank bigint
)
language plpgsql
stable
security invoker
set search_path = public
as $$
begin
    if not exists (
        select 1 from public.community_group_members m
        where m.group_id = p_group_id and m.user_id = auth.uid()
    ) then
        raise exception 'You are not a member of this community group';
    end if;

    return query
    with totals as (
        select m.user_id, coalesce(sum(e.points), 0)::bigint as total_points
        from public.community_group_members m
        left join public.sustainability_point_events e
          on e.user_id = m.user_id
         and (p_period_start is null or e.awarded_at >= p_period_start)
        where m.group_id = p_group_id
        group by m.user_id
    ), ranked as (
        select t.user_id, coalesce(nullif(trim(p.display_name), ''), 'LiFE360 User') as display_name,
               t.total_points,
               dense_rank() over (order by t.total_points desc) as rank
        from totals t
        left join public.profiles p on p.id = t.user_id
    )
    select r.user_id, r.display_name, r.total_points, r.rank
    from ranked r
    order by r.rank, r.display_name;
end;
$$;

revoke all on function public.life360_global_leaderboard(timestamptz) from public;
revoke all on function public.life360_group_leaderboard(uuid, timestamptz) from public;
grant execute on function public.life360_global_leaderboard(timestamptz) to authenticated;
grant execute on function public.life360_group_leaderboard(uuid, timestamptz) to authenticated;

-- Secure community lifecycle RPCs. These functions never expose arbitrary
-- membership writes to the mobile client.
create or replace function public.life360_create_group(p_name text)
returns table (id uuid, name text, invite_code text)
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
    v_group_id uuid;
    v_code text;
begin
    if auth.uid() is null then raise exception 'Authentication required'; end if;
    if char_length(trim(coalesce(p_name, ''))) not between 2 and 80 then
        raise exception 'Community name must be 2 to 80 characters';
    end if;
    v_code := upper(substr(encode(gen_random_bytes(8), 'hex'), 1, 10));
    insert into public.community_groups(name, invite_code, created_by)
    values (trim(p_name), v_code, auth.uid()) returning community_groups.id into v_group_id;
    insert into public.community_group_members(group_id, user_id) values (v_group_id, auth.uid());
    return query select g.id, g.name, g.invite_code from public.community_groups g where g.id = v_group_id;
end;
$$;

create or replace function public.life360_join_group(p_invite_code text)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare v_group_id uuid;
begin
    if auth.uid() is null then raise exception 'Authentication required'; end if;
    select id into v_group_id from public.community_groups
      where invite_code = upper(trim(coalesce(p_invite_code, '')));
    if v_group_id is null then raise exception 'Invalid invite code'; end if;
    insert into public.community_group_members(group_id, user_id)
      values (v_group_id, auth.uid()) on conflict do nothing;
    return v_group_id;
end;
$$;

create or replace function public.life360_my_groups()
returns table (id uuid, name text, invite_code text)
language sql
stable
security invoker
set search_path = public
as $$
    select g.id, g.name, g.invite_code
    from public.community_groups g
    join public.community_group_members m on m.group_id = g.id
    where m.user_id = auth.uid()
    order by g.name;
$$;

revoke all on function public.life360_create_group(text) from public;
revoke all on function public.life360_join_group(text) from public;
revoke all on function public.life360_my_groups() from public;
grant execute on function public.life360_create_group(text) to authenticated;
grant execute on function public.life360_join_group(text) to authenticated;
grant execute on function public.life360_my_groups() to authenticated;
