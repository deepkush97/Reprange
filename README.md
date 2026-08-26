<p align="center">
  <h1 align="center">🏋️ Reprange</h1>
  <p align="center"><i>Log it. Beat it.</i></p>
  <p align="center">A fast, keyboard-free workout tracker for Android — built with Jetpack Compose and Material 3 Expressive.</p>
</p>

---

Reprange is a native Android workout logging app designed for the gym: big tap targets,
no keyboard in the common path, automatic rest timers, and a 1,324-exercise library with
animations. Dark-theme first, fully edge-to-edge, and themed by Material You.

## Screenshots

| | | |
|---|---|---|
| ![Home](screenshots/01_home_light.png) | ![Library](screenshots/02_library_light.png) | ![Search](screenshots/03_library_search.png) |
| *Home — start in one tap* | *Library — 1,324 exercises* | *Full-text search* |
| ![Detail](screenshots/04_exercise_detail_light.png) | ![Workout](screenshots/05_active_workout_light.png) | ![Builder](screenshots/06_template_builder_light.png) |
| *Exercise detail hero* | *Active workout — stepper rows* | *Routine builder* |
| ![Progress](screenshots/07_progress_light.png) | ![Settings](screenshots/08_settings_light.png) | ![Dark](screenshots/09_home_dark.png) |
| *Progress & PRs* | *Settings* | *Dark mode* |

<p align="center">
  <img src="screenshots/10_progress_dark.png" width="260"> <img src="screenshots/11_exercise_detail_dark.png" width="260"> <img src="screenshots/12_active_workout_dark.png" width="260">
</p>
<p align="center"><i>Dark theme: progress, exercise detail, active workout</i></p>

## Features

### 🏋️ Frictionless workout logging
- **Stepper set rows** — ±2.5 kg / ±1 rep in one tap; tap a value for fine input.
  Values pre-fill from your last session, so a typical set is **one tap on ✓**
- **Set types per row** — tap the row icon to cycle Normal 🏋️ / Warmup 🔥 / Drop set 📉 / Failure ⚡
- **Planned sets** — start a saved routine and its sets appear as pre-filled rows
  (targets included); completed rows flip to logged, `Add set` duplicates the last one
- **Auto rest timer** — starts when a Normal set is logged; +15 s / skip from the top bar
- **PR detection** — every logged set is compared against your history (Epley estimated
  1RM); new records trigger a celebration haptic + snackbar
- Swipe-to-delete on logged sets; elapsed timer and live volume totals

### 📋 Plan ahead (routines)
- Build routines once (name, notes, exercises, sets, set strategy) and start them in one tap
- Set strategies: **Standard, Step-up (pyramid — auto-escalates +2.5 kg per set),
  Drop set, Super set, Failure**

### 📚 Exercise library
- **1,324 exercises** ingested at first launch from the
  [hasaneyldrm/exercises-dataset](https://github.com/hasaneyldrm/exercises-dataset)
  (Retrofit + kotlinx.serialization → Room)
- Full-text search (Room FTS4) across name, target, and equipment
- Filters by body part and difficulty; every exercise has an animation GIF,
  targeted muscles, equipment, and step-by-step instructions
- Exercise detail is a **hero surface**: the artwork drives a dynamic gradient
  background and (optionally) re-seeds the entire app palette live

### 📈 Progress
- Weekly volume chart (last 10 rolling weeks), weekly sets, PR count
- Personal-record table with best set and estimated 1RM per exercise

### 🎨 Material 3 Expressive
- `MaterialExpressiveTheme` + expressive motion scheme; smooth-corner squircles
- Three-tier color seeding: **wallpaper (Monet) → 19 preset accents → content-derived
  live seeding** from exercise artwork; pure-black AMOLED mode
- Dynamic hero backgrounds (gradient / glow / blur / layered / mesh) with adaptive
  foregrounds and status-bar icon flipping
- Tiered haptic feedback (primitive compositions → predefined effects → view constants)
  behind a single `AppHaptics` choke point, with an in-app toggle

## Tech stack

| Layer | Choice |
|---|---|
| Language | 100% Kotlin |
| UI | Jetpack Compose, **Material 3 `1.5.0-alpha23`** (Expressive), Coil 3 (GIFs), Haze, Palette, materialKolor |
| Architecture | Clean Architecture + MVVM (ViewModels, use cases, repository layer) |
| Data | Room (FTS4 search, relations, cascades), DataStore preferences |
| Networking | Retrofit + OkHttp + kotlinx.serialization (dataset ingest & seeding) |
| DI | Hilt |
| Navigation | Compose Navigation with type-safe routes |
| Build | AGP 9.2.1 · Gradle 9.4.1 · Kotlin 2.3.21 (min SDK 26, target 37) |

## Architecture

```
app/src/main/kotlin/com/deepkush/reprange/
├── data/
│   ├── db/          # Room: entities, FTS index, DAOs, relations
│   ├── remote/      # Retrofit API + DTOs for the exercises dataset
│   └── repo/        # Repositories + first-launch dataset seeder
├── domain/usecase/  # CreateWorkoutPlan, StartSession, LogCompletedSet,
│                    # CalculateProgress (volume buckets, Epley 1RM, PRs)
├── workout/         # ActiveSessionManager: live session + rest timer,
│                     # survives navigation & process death (Room-backed)
├── di/              # Hilt modules (database, network)
├── ui/
│   ├── theme/       # AppTheme (hybrid seeding), Type, ColorExtractor
│   ├── component/   # Dynamic background engine, sheet host, dialogs,
│   │                # settings groups, bottom bar, session dock
│   └── screens/     # home · library · exercise · workout · template ·
│                    # progress · settings
└── utils/           # AppHaptics, DataStore prefs, shapes, formatters
```

## How it works

- **Search** — Room FTS4 external-content index over `name / category / target /
  equipment`, prefix queries (`barbell*`), joined back on `rowid`
- **Weekly volume** — Σ(weight × reps) of Normal sets from finished sessions,
  bucketed into the last 10 rolling 7-day windows
- **Estimated 1RM** — Epley: `weight × (1 + reps / 30)`; PRs compare a weighted
  1RM + volume score against the exercise's history
- **Live session** — an unfinished session is persisted with `ended_at = NULL`,
  so it survives process death and re-attaches on next launch (mini-dock above
  the nav bar)

## Build & run

```bash
# Requirements: Android Studio (Narwhal+), JDK 17+, Android SDK 37
git clone <this-repo>
cd Reprange
./gradlew :app:assembleDebug
# or open in Android Studio and press Run
```

First launch fetches the exercise dataset over the network (a few MB) and seeds
the local database — an error banner with retry appears if offline.

## Dataset & media attribution

- Exercise metadata, translations, and structure:
  [hasaneyldrm/exercises-dataset](https://github.com/hasaneyldrm/exercises-dataset) — MIT
- Exercise thumbnails & animation GIFs: **© [Gym visual](https://gymvisual.com/)**,
  redistributed via that dataset under its media terms; attribution is shown
  on every exercise detail screen

## License

App code: MIT. Third-party media terms apply as noted above.
