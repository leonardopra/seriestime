# SeriesTime

Mobile app (Android + iOS) to track TV shows and movies, TVTime-style: what you're watching, what you've watched and what you want to watch, with episode check-off and TVTime data import.

## Stack

- **Kotlin Multiplatform + Compose Multiplatform** — shared Android/iOS UI
- **Supabase** — email/password authentication and Postgres database (with Row Level Security)
- **TMDB** — movie and TV show search and metadata (IT/EN localization)

## Features

- Personal account with persistent session
- Movie and TV show search on TMDB
- Lists: watchlist / watching / watched, favorites
- Show detail with episode check-off, "mark season/series watched" and progress counters
- Movie detail with watched, favorite and rewatch counter
- TVTime JSON export import (matching via IMDB/TVDB IDs, unmatched report, re-runnable)

## Setup

1. Create `local.properties` in the project root:

   ```properties
   sdk.dir=/path/to/Android/sdk
   supabase.url=...        # Supabase project URL
   supabase.anonKey=...    # anon key
   tmdb.accessToken=...    # TMDB API key (v3) or Read Access Token (v4)
   ```

2. Apply the database schema: `supabase/migrations/0001_init.sql` (via `supabase db push` or the SQL editor).

3. Build:
   - **Android**: `./gradlew :composeApp:assembleDebug`
   - **iOS**: open `iosApp/iosApp.xcodeproj` in Xcode and run (full Xcode required)

Tests: `./gradlew :composeApp:testDebugUnitTest`

---

This product uses the TMDB API but is not endorsed or certified by TMDB.
