-- LiFE360 energy readings for ESP32 + INA219.
-- Review in Supabase SQL Editor before running. Do not put service-role keys on the ESP32 or Android app.

create table if not exists public.energy_readings (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    device_id text not null,
    voltage_v numeric(12, 4) not null check (voltage_v >= 0),
    current_ma numeric(12, 4) not null,
    power_mw numeric(14, 4) not null,
    energy_wh numeric(14, 6),
    measured_at timestamptz not null default now(),
    reading_source text not null default 'esp32_ina219'
        check (reading_source in ('esp32_ina219', 'manual_test')),
    created_at timestamptz not null default now()
);

create index if not exists idx_energy_readings_user_time
    on public.energy_readings (user_id, measured_at desc);

create index if not exists idx_energy_readings_device_time
    on public.energy_readings (device_id, measured_at desc);

alter table public.energy_readings enable row level security;

-- Users can read only their own device readings.
drop policy if exists "Users can read own energy readings" on public.energy_readings;
create policy "Users can read own energy readings"
    on public.energy_readings
    for select
    to authenticated
    using (auth.uid() = user_id);

-- Intentionally no public/anonymous INSERT policy. ESP32 ingestion should go through
-- a Supabase Edge Function that validates a per-device secret and assigns user_id.
