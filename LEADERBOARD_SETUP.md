# LiFE360 Points & Leaderboard — setup notes

## What was added
- Android Compose leaderboard screen reachable from the transportation dashboard.
- Global and Community views, each with Weekly and All-time filters.
- Community creation and invite-code joining.
- Supabase repository models/RPC calls.
- SQL functions for community create/join/list and leaderboard queries.

## Required backend setup
1. Open Supabase Dashboard → SQL Editor.
2. Run `supabase/migrations/002_points_and_leaderboards.sql`.
3. Ensure the existing `profiles` table has `id` and `display_name` as expected by the leaderboard query.
4. Keep point-event INSERT permissions disabled for the mobile client.

## Important current limitation
The leaderboard reads verified point events, but this project does not yet include a trusted Edge Function that awards points after validating journeys, meals, energy readings, or challenges. Therefore a new account may see an empty leaderboard. Do not add a client-side insert policy or let the app award its own points. Implement and test server-side award rules before counting activity.

## Build
Open the project in Android Studio on the development PC, ensure its Android SDK and Gradle dependencies are installed, restore the local `local.properties` file from the developer's private setup (do not share it), then run `:app:assembleDebug`. The source package intentionally excludes local credentials and build outputs.

The source was not compiled in this environment: Gradle 9.6.0 was not cached and this environment could not reach services.gradle.org. A working APK is therefore not included or claimed.
