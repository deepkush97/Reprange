# Onboarding — Recommended Routines & Exercise Suggestions

**Date:** 2026-08-31  
**Status:** Approved  
**Author:** Reprange — brainstormed with user (Goal: All three dimensions, Ask split choice, Preview + swap, Optional card)  
**Related:** `docs/superpowers/specs/2026-08-26-workout-card-stepper-rows-design.md`, `ReprangeRoot.kt`, `DatasetSeeder.kt`, `Entities.kt`

## 1. Summary

Add an optional onboarding wizard that asks 5 questions (Goal, Experience, Equipment, Days/week, Split) and generates 1-3 personalized `WorkoutTemplate` routines from the existing 1,324-exercise dataset, shown in a preview where any exercise can be swapped before batch saving. Entry is an optional Home card (`ONBOARDING_COMPLETED == false`) and a retrigger row in Settings; never a gate.

## 2. Goals / Non-Goals

**Goals:**
- Cold-start user leaves onboarding with usable routines on day one.
- Offline, deterministic, reuses existing DB/DAOs (no backend, no LLM).
- Preview gives agency: see exercises, swap per row, then save.

**Non-Goals:**
- No AI generation, no per-user ML ranking (future layer).
- No new exercise curation beyond the 1,324 dataset + `Difficulty.derive`.
- No mandatory onboarding; empty Home remains valid.

## 3. User Flow

```
Home (card: "Personalize your routines?" if !completed) ──tap──> OnboardingRoute
  1. Goal          [Strength | Hypertrophy | Fat loss | General]  → GoalConfig
  2. Experience    [Beginner | Intermediate | Advanced]           → maxDifficulty
  3. Equipment     [Bodyweight | Dumbbells | Barbell+Dumbbells | Full gym]
  4. Days/week     stepper 2..6 (haptics, +/−)
  5. Split         filtered by Days: 2→[FullBody], 3→[FullBody,PPL],
                                         4→[UpperLower,FullBody], 5→[PPL,UpperLower+PPL],
                                         6→[PPL,PPL×2]  (default = first valid)
     ──Continue──> Preview (tabs per template, horizontal swipe / SegmentedButton)
                   Each template: header "Push — 5 exercises · 3×10 · 90s" + list of ExerciseRow
                   Tap row → ExercisePickerScreen(mode="template", pre-filtered by bucket category)
                   → swap in place
  Footer: Skip (always) → mark completed + popBackStack
          Create N routines (primary, shows spinner) → batch `CreateWorkoutPlanUseCase`
          → mark completed → popBackStack to Home (now shows generated templates)
Settings > Personalize routines → same wizard (popUpTo Home, restores state)
```

Pager: progress dots, Skip always visible, Continue primary. No keyboard in common path.

## 4. Architecture

```
ui/screens/onboarding/
  OnboardingScreen.kt          // NavHost-style pager, owns OnboardingViewModel
  OnboardingViewModel.kt       // MutableStateFlow<OnboardingProfile>, currentStep, preview
  components/OnboardingGoalCard.kt etc.

domain/onboarding/
  OnboardingRecommender.kt     // pure Kotlin, JVM-testable: (Profile, GoalConfig, Split) -> List<PreviewTemplate>
  OnboardingProfile.kt         // data class aggregating 5 answers + derived allowedEquipment/maxDifficulty
  GoalConfig.kt                // data class(sets, repRange, restSeconds, defaultStrategy)

constants/PreferenceKeys.kt    // 6 new keys (see §5)
navigation/Routes.kt           // data object OnboardingRoute
ReprangeRoot.kt                // startDestination stays HomeRoute; chrome hide on OnboardingRoute
HomeScreen.kt                  // optional card
SettingsScreen.kt              // "Personalize routines" row
```

**Chrome:** `OnboardingRoute` hides `AppBottomBar`/`SessionDock` via existing `routeIndex==-1` check (same as `ExercisePicker`/`ActiveWorkout`). Inside `AppTheme` + `Surface(background)` so dark mode/pure black/seeds apply.

## 5. Data Model

No new tables. 6 new DataStore keys in `constants/PreferenceKeys.kt` (same pattern as `DARK_MODE`):

- `ONBOARDING_COMPLETED: booleanPreferencesKey("onboarding_completed")` — default `false`
- `ONBOARDING_GOAL: stringPreferencesKey("onboarding_goal")` — `Goal` enum
- `ONBOARDING_EXPERIENCE: stringPreferencesKey("onboarding_experience")` — `Experience` enum
- `ONBOARDING_EQUIPMENT: stringPreferencesKey("onboarding_equipment")` — `EquipmentProfile` enum
- `ONBOARDING_DAYS: intPreferencesKey("onboarding_days")` — 2..6, default 3
- `ONBOARDING_SPLIT: stringPreferencesKey("onboarding_split")` — `Split` enum

Consumed via existing `rememberEnumPreference` / `rememberPreference`. `OnboardingProfile` aggregates and exposes `allowedEquipment: Set<String>` and `maxDifficulty: Difficulty`.

Templates remain `WorkoutTemplateEntity(title, notes, createdAt)` + `TemplateExerciseEntity` — written via unchanged `CreateWorkoutPlanUseCase`.

## 6. Recommendation Engine (Isolated Unit)

`OnboardingRecommender` interface:

```kotlin
data class PreviewTemplate(val title: String, val items: List<PlanItem>)
fun recommend(profile: OnboardingProfile, goal: GoalConfig, split: Split): List<PreviewTemplate>
```

**Split definitions** (muscle-bucket → exercise count):

- `FullBody (6)`: Chest 1, Back 1, Shoulders 1, upper legs 1, waist 1, Arms 1
- `UpperLower (5+5)`: Upper: Chest 1, Back 1, Shoulders 1, Arms 2; Lower: upper legs 2, lower legs 1, waist 2
- `PPL (5/5/5)`: Push: Chest 2, Shoulders 2, Triceps 1; Pull: Back 2, Biceps 2, rear delts 1; Legs: upper legs 2, lower legs 1, waist 2
- `PPL_UL` variants split similarly

**Per-bucket query:** `ExerciseDao` by `target`/`muscle_group`/`category` + `equipment IN allowed` + `difficulty <= maxDifficulty`, `ORDER BY name LIMIT bucketSize+2`. Deduplicate across buckets; if bucket empty, relax one difficulty tier. Title set as `"Push — Recommended"` etc. `PlanItem` fields: `setCount`/`restSeconds`/`strategy` from `GoalConfig` (`STEP_UP` for Chest/Back/upper legs when Experience != BEGINNER, else `STANDARD`).

**Goal configs:**

| Goal | Sets | Reps | Rest | Strategy |
|---|---|---|---|---|
| Strength | 3-5 | 5 | 90s | STEP_UP |
| Hypertrophy | 3 | 8-12 | 90s | STANDARD |
| Fat loss | 3 | 12-15 | 60s | circuit (rest 60s) |
| General | 3 | 10 | 90s | STANDARD |

Unit is pure — no Android deps — so JVM testing is trivial.

## 7. Preview & Edit

Preview screen = read-only `SettingsGroup`-style list per template (tabbed via `SegmentedButton` or swipe pager). Row tap → `ExercisePickerScreen` with `category` pre-filled to the bucket's target so the list is relevant; on `savedStateHandle["picked_exercise"]` the preview's `PlanItem.exerciseId` is replaced. Header: `"Push — 5 exercises · 3×10 · 90s"`. Empty bucket → placeholder `+ Add exercise`. Batch save loops `CreateWorkoutPlanUseCase` with spinner.

## 8. Error Handling & Edge Cases

- Seed not done: onboarding queries wait on `ExerciseRepository.seedState`; `SeedState.Loading` shows spinner, `Error` shows retry card (same as `LibraryScreen`).
- Empty bucket: fallback one difficulty tier; still empty → skip bucket, show placeholder.
- Re-entry: regenerates previews; only writes on explicit `Create` tap — idempotent.
- Skip: marks `ONBOARDING_COMPLETED=true` without writing templates (card disappears, can retrigger).
- Days→Split recompute live: changing Days from 4→3 while Split=UpperLower auto-resets to FullBody.

## 9. Testing

- **JVM:** `OnboardingRecommenderTest` — profile × expected template counts, equipment filtering (bodyweight profile never returns `barbell` equipment), deduplication, empty-bucket fallback.
- **Instrumented:** `CreateWorkoutPlanUseCase` with `inMemoryDatabaseBuilder` inserting `PreviewTemplate` lists.
- **Compose:** Home card visibility (`!completed` shows, completed hides), Preview swap flow.
