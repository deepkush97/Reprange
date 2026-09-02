# Final Fix Report — Final Review Findings (I1, I2, I3)

Date: 2026-09-02
Branch: onboarding-recommended-routines (HEAD 3198306 -> fix)
Review diff: `review-13f9095..3198306.diff` (findings I1-I3)

## Summary
Fixed all 3 final review findings, verified with covering tests, committed.

## Findings Fixed

### I1 — Preview shows raw exerciseId not exercise name + violates "Reuse ExerciseRow"
**File:** `app/src/main/kotlin/com/deepkush/reprange/ui/screens/onboarding/OnboardingScreen.kt:1374` (was `PreviewExerciseRow` rendering `Text(exerciseId)`)
**Fix:**
- `OnboardingViewModel` now exposes `StateFlow<Map<String, ExerciseEntity>> exerciseMap` (line 43-44). Populated via `refreshExerciseMap()` after `generatePreview()` and incremental `fetchAndCacheExercise(id)` after `swapExercise`/`addExercise` (using `ExerciseRepository.getExercises`/`getExercise`). Uses `associateBy` caching.
- `OnboardingScreen` collects `exerciseMap` via `collectAsStateWithLifecycle()` and for each preview item does `val exercise = remember(item.exerciseId, exerciseMap) { exerciseMap[item.exerciseId] }` to avoid repeated queries per recomposition.
- `PreviewExerciseRow` signature changed to `PreviewExerciseRow(exerciseId: String, exercise: ExerciseEntity?, ...)` . Renders `exercise?.name ?: exerciseId` as title with `remember`, subtitle `muscleGroup · equipment`, and two `AssistChip` badges for `equipment` and `muscleGroup` plus plan details (`sets · reps · rest · strategy`). Reuses `ExerciseRow` visual pattern: `Card(shape=RoundedCornerShape(12.dp), colors=surfaceVariant alpha 0.25f)` with `IconButton(SwapHoriz)`. Raw id only shown as fallback when `exercise == null` (subtitle "Unknown exercise").
- Added `remember` for title/subtitle to avoid recomputation.

### I2 — Business logic (batch save) lives in Composable via EntryPointAccessors
**File:** `app/src/main/kotlin/com/deepkush/reprange/ui/screens/onboarding/OnboardingScreen.kt:1133` (`EntryPointAccessors.fromApplication + context.dataStore.edit`)
**Fix:**
- Removed `OnboardingEntryPoint` interface, `dagger.hilt.EntryPoint`/`InstallIn`/`EntryPointAccessors` imports, `dataStore`/`edit` imports, dead `persistAndFinish()` function, and local `isSaving`/`previewError`/`isGeneratingPreview` mutable states from composable.
- `OnboardingViewModel` now injects `CreateWorkoutPlanUseCase? = null` and `@ApplicationContext Context? = null` (nullable with defaults for test backward-compatibility; Hilt provides non-null in production). Exposes:
  - `StateFlow<Boolean> isSaving`, `StateFlow<Boolean> isGeneratingPreview`, `StateFlow<String?> previewError` with `clearPreviewError()`
  - `suspend fun generatePreview()` now sets `_isGeneratingPreview`/`_previewError` and calls `refreshExerciseMap()`
  - `suspend fun saveAll(): Result<Unit>` — validates preview non-empty, loops `createWorkoutPlanUseCase(null, tmpl.title, "", tmpl.items)` for each template, then `appContext.dataStore.edit { prefs[ONBOARDING_COMPLETED]=true; prefs[ONBOARDING_GOAL/EXPERIENCE/EQUIPMENT/DAYS/SPLIT] }`, manages `_isSaving`/`_previewError`
  - `suspend fun skipOnboarding()` — sets `ONBOARDING_COMPLETED=true`
- `OnboardingScreen` now only calls `viewModel.saveAll()` and `viewModel.skipOnboarding()`, observing `isSaving`/`previewError`/`isGeneratingPreview` via `collectAsStateWithLifecycle()`. Footer "Create routines" button does `scope.launch { val result = viewModel.saveAll(); if (result.isSuccess) onFinish() else snackbar }` with spinner when `isSaving`. Wizard "Skip" button does `viewModel.skipOnboarding(); onFinish()`. Wizard "Continue/Preview" uses `viewModel.generatePreview()` + `clearPreviewError()`. No `EntryPointAccessors` remains. Verified `grep EntryPointAccessors` returns 0 hits in `OnboardingScreen.kt`.

### I3 — debug/AndroidManifest.xml adds duplicate LAUNCHER intent-filter
**File:** `app/src/debug/AndroidManifest.xml:3`
**Fix:** Removed `<intent-filter>` block (action MAIN + category LAUNCHER), kept activity declaration alone as self-closing `<activity android:name="androidx.activity.ComponentActivity" ... />` with comment retained. Robolectric only needs manifest merger to resolve `ComponentActivity`.

## Verification

```bash
./gradlew :app:testDebugUnitTest --tests "com.deepkush.reprange.domain.onboarding.OnboardingRecommenderTest" \
  --tests "com.deepkush.reprange.ui.home.HomeOnboardingCardTest" \
  --tests "com.deepkush.reprange.ui.onboarding.OnboardingViewModelTest" \
  --tests "com.deepkush.reprange.ui.onboarding.PreviewSwapTest"
# -> BUILD SUCCESSFUL (14s, 36 tasks)
./gradlew :app:testDebugUnitTest --tests "com.deepkush.reprange.domain.onboarding.* \
  com.deepkush.reprange.ui.onboarding.* com.deepkush.reprange.ui.home.HomeOnboardingCardTest"
# -> BUILD SUCCESSFUL (3s)
```

No regressions. Tests cover: recommender rule-based logic (equipment/difficulty/split buckets), HomeOnboardingCard visibility/click, ViewModel stepper/profile/preview/swap, Preview swap.

## Commit
Staged and committed 3 modified files.

```
fix(onboarding): preview entity lookup, ViewModel batch save, debug manifest — I1 I2 I3
- I1: PreviewExerciseRow now resolves ExerciseEntity via ViewModel exerciseMap (remember-cached) and renders name + muscleGroup + equipment badge (AssistChip) with raw id fallback; reuses ExerciseRow card pattern
- I2: Move batch save + DataStore persist from Composable EntryPointAccessors to OnboardingViewModel.saveAll()/skipOnboarding() with isSaving/previewError StateFlows; UI only observes and calls ViewModel
- I3: Remove LAUNCHER intent-filter from debug AndroidManifest, keep ComponentActivity declaration for Robolectric
```

## Files Changed
- `app/src/debug/AndroidManifest.xml`
- `app/src/main/kotlin/com/deepkush/reprange/ui/screens/onboarding/OnboardingViewModel.kt`
- `app/src/main/kotlin/com/deepkush/reprange/ui/screens/onboarding/OnboardingScreen.kt`
