# LiFE360 development plan

## Existing code reviewed
- Android Kotlin + Jetpack Compose app
- Supabase Auth and PostgREST client
- GPS location updates and distance/duration display
- JourneyInsert model and JourneyRepository

## First source correction
The original Stop Journey button stopped GPS but did not call JourneyRepository.saveJourney().
The updated MainActivity now captures the journey timestamps and first/last accurate GPS fixes,
then attempts to insert the journey for the currently authenticated user. The UI reports success
or the error returned by the save operation.

This source change has not yet been compiled or tested against the user's Supabase project.
It must be built and tested locally before being treated as complete.

## Hardware plan: ESP32 + INA219
- Read bus voltage, shunt/current and power from INA219 over I2C.
- Integrate power over time to calculate energy (Wh); preserve timestamps and device ID.
- Send readings over Wi-Fi to a Supabase Edge Function using a per-device credential.
- Never embed Supabase service-role credentials in firmware or client applications.
- Display real readings separately from sample/demo values.

## Software milestones
1. Build and test journey persistence on Android and verify a row in Supabase.
2. Add a journey history query/UI and transport-mode user confirmation.
3. Add energy_readings schema and secure device ingestion endpoint.
4. Build an ESP32 firmware test harness; validate against the physical INA219 tomorrow.
5. Add food photo capture, recognition provider integration, manual correction, nutrition source and estimate labels.
6. Integrate dashboards, reports, challenges and advisor.
7. Test on a physical Android phone, then build a release APK.

## Git safety
Do not commit local.properties, .env files, API secrets, signing keys, or Supabase service-role keys.

## Points and leaderboards (added 2026-11-04)
- Added `supabase/migrations/002_points_and_leaderboards.sql` for point events, private college/community groups, and global/group leaderboard RPCs.
- Supports all-time rankings (`p_period_start = null`) and weekly rankings (pass the UTC start timestamp for the current week).
- Point-event writes are intentionally blocked from ordinary clients. A trusted Supabase Edge Function must validate source activity and award points; otherwise users could manipulate their scores.
- The migration has not been applied to the user's Supabase project. The RPCs and RLS policies need validation against the actual project before use.
- Android leaderboard UI, group creation/join flow, and verified point-award Edge Function are not yet implemented or tested. Do not describe this feature as live in an APK until those steps and a device build are complete.
