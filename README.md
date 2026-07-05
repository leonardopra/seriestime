# SeriesTime

App mobile (Android + iOS) per tenere traccia di serie TV e film, in stile TVTime: cosa stai guardando, cosa hai visto e cosa vuoi vedere, con spunta degli episodi e import dei dati da TVTime.

## Stack

- **Kotlin Multiplatform + Compose Multiplatform** — UI condivisa Android/iOS
- **Supabase** — autenticazione email/password e database Postgres (con Row Level Security)
- **TMDB** — ricerca e metadati di film e serie (localizzati IT/EN)

## Funzionalità

- Account personale con sessione persistente
- Ricerca film e serie su TMDB
- Liste: da vedere / in corso / visti, preferiti
- Dettaglio serie con spunta episodi, "segna stagione/serie vista" e conteggio progressi
- Dettaglio film con visto, preferito e contatore rivisioni
- Import dell'export JSON di TVTime (matching via ID IMDB/TVDB, report dei non abbinati, riesegubile)

## Setup

1. Crea `local.properties` nella root:

   ```properties
   sdk.dir=/path/to/Android/sdk
   supabase.url=...        # URL del progetto Supabase
   supabase.anonKey=...    # anon key
   tmdb.accessToken=...    # API key TMDB (v3) o Read Access Token (v4)
   ```

2. Applica lo schema al database: `supabase/migrations/0001_init.sql` (via `supabase db push` o SQL editor).

3. Build:
   - **Android**: `./gradlew :composeApp:assembleDebug`
   - **iOS**: apri `iosApp/iosApp.xcodeproj` in Xcode e avvia (serve Xcode completo)

Test: `./gradlew :composeApp:testDebugUnitTest`

---

Questo prodotto usa l'API di TMDB ma non è approvato né certificato da TMDB.
