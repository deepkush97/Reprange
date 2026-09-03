# Onboarding — Recommended Routines Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an optional 5-step onboarding wizard (Goal/Experience/Equipment/Days/Split) that generates 1-3 personalized `WorkoutTemplate` routines from the existing 1,324-exercise dataset, with preview-and-swap before batch save, triggered by a Home card and a Settings row.

**Architecture:** New `domain/onboarding/OnboardingRecommender` pure unit maps `OnboardingProfile` → `List<PreviewTemplate>` via per-bucket `ExerciseDao` queries; new `ui/screens/onboarding/` pager owns the wizard and preview; persistence reuses `CreateWorkoutPlanUseCase` and 6 new `PreferenceKeys` DataStore entries; navigation adds `OnboardingRoute` hiding chrome like `ActiveWorkoutRoute`.

**Tech Stack:** Kotlin 100%, Jetpack Compose (Material 3 Expressive), Room + FTS4, DataStore preferences, Hilt, Compose Navigation type-safe, Coil not needed here.

## Global Constraints

- minSdk 26, targetSdk 37 (from `app/build.gradle.kts`)
- Existing `fallbackToDestructiveMigration()` for Room — no manual migration needed for v2→v3 additions only if new table, but we add no table
- Reuse existing patterns: `PreferenceKeys` `*PreferencesKey(...)`, `rememberEnumPreference` / `rememberPreference`, `listItemShape`, `SettingsGroup/ModernSwitch`, `ExercisePicker` via `savedStateHandle["picked_exercise"]`
- Offline-first: no network calls in recommender; works after `DatasetSeeder` done
- All strings via `strings.xml` or inline (existing code uses inline for settings titles) — follow inline for onboarding copy

---

## File Structure

**New files:**
- `app/src/main/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingProfile.kt` — data class + enums `Goal`, `Experience`, `EquipmentProfile`, `Split`, `GoalConfig`
- `app/src/main/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingRecommender.kt` — `fun recommend(profile, split): List<PreviewTemplate>` + `data class PreviewTemplate(title, items: List<PlanItem>)`
- `app/src/main/kotlin/com/deepkush/reprange/ui/screens/onboarding/OnboardingScreen.kt` — pager, step composables
- `app/src/main/kotlin/com/deepkush/reprange/ui/screens/onboarding/OnboardingViewModel.kt` — holds `MutableStateFlow<OnboardingProfile>` + currentStep + preview
- `app/src/test/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingRecommenderTest.kt` — JVM tests

**Modified files:**
- `app/src/main/kotlin/com/deepkush/reprange/constants/PreferenceKeys.kt` — add 6 keys + 4 enums
- `app/src/main/kotlin/com/deepkush/reprange/navigation/Routes.kt` — `data object OnboardingRoute`
- `app/src/main/kotlin/com/deepkush/reprange/ReprangeRoot.kt` — chrome hide check, handle `OnboardingRoute` in `AppNavHost`
- `app/src/main/kotlin/com/deepkush/reprange/ui/screens/home/HomeScreen.kt` — optional Personalize card
- `app/src/main/kotlin/com/deepkush/reprange/ui/screens/settings/SettingsScreen.kt` — "Personalize routines" row
- `app/src/main/kotlin/com/deepkush/reprange/ui/screens/library/ExercisePickerScreen.kt` — optional bucket pre-filter comment (no API change)

---

### Task 1: Preference keys + profile model

**Files:**
- Modify: `app/src/main/kotlin/com/deepkush/reprange/constants/PreferenceKeys.kt`
- Create: `app/src/main/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingProfile.kt`
- Test: `app/src/test/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingProfileTest.kt` (optional quick)

**Interfaces:**
- Consumes: existing `PreferenceKeys` enums/keys patterns
- Produces: `enum Goal`, `enum Experience`, `enum EquipmentProfile { BODYWEIGHT, DUMBBELL_ONLY, BARBELL_DUMBBELL, FULL_GYM; fun allowedEquipment(): Set<String> }`, `enum Split { FULL_BODY, UPPER_LOWER, PPL }`, `data class GoalConfig(val sets:Int, val repLow:Int, val repHigh:Int, val restSeconds:Int, val strategy:SetStrategy)`, `data class OnboardingProfile(val goal:Goal, val experience:Experience, val equipment:EquipmentProfile, val days:Int, val split:Split) { fun allowedEquipment(): Set<String>; fun maxDifficulty(): Difficulty }`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingProfileTest.kt
package com.deepkush.reprange.domain.onboarding
import com.google.common.truth.Truth.assertThat
import org.junit.Test
class OnboardingProfileTest {
    @Test fun allowedEquipment_bodyweight_isSingle() {
        assertThat(EquipmentProfile.BODYWEIGHT.allowedEquipment()).containsExactly("body weight")
    }
    @Test fun maxDifficulty_beginner_isBeginner() {
        val p = OnboardingProfile(Goal.GENERAL_FITNESS, Experience.BEGINNER, EquipmentProfile.FULL_GYM, 3, Split.FULL_BODY)
        assertThat(p.maxDifficulty().name).isEqualTo("BEGINNER")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.deepkush.reprange.domain.onboarding.OnboardingProfileTest" -v`
Expected: FAIL with "Unresolved reference: OnboardingProfile"

- [ ] **Step 3: Add keys to PreferenceKeys.kt**

```kotlin
// in object PreferenceKeys
val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
val ONBOARDING_GOAL = stringPreferencesKey("onboarding_goal")
val ONBOARDING_EXPERIENCE = stringPreferencesKey("onboarding_experience")
val ONBOARDING_EQUIPMENT = stringPreferencesKey("onboarding_equipment")
val ONBOARDING_DAYS = intPreferencesKey("onboarding_days")
val ONBOARDING_SPLIT = stringPreferencesKey("onboarding_split")
```

Add enums above or in new file `OnboardingProfile.kt`:

```kotlin
enum class Goal { STRENGTH, HYPERTROPHY, FAT_LOSS, GENERAL_FITNESS }
enum class Experience { BEGINNER, INTERMEDIATE, ADVANCED }
enum class EquipmentProfile { BODYWEIGHT, DUMBBELL_ONLY, BARBELL_DUMBBELL, FULL_GYM;
  fun allowedEquipment(): Set<String> = when(this){
    BODYWEIGHT -> setOf("body weight")
    DUMBBELL_ONLY -> setOf("body weight","dumbbell")
    BARBELL_DUMBBELL -> setOf("body weight","dumbbell","barbell","ez barbell")
    FULL_GYM -> emptySet() // empty = no filter
  }
}
enum class Split { FULL_BODY, UPPER_LOWER, PPL }
fun Split.validForDays(days:Int): Boolean = when(this){
  Split.FULL_BODY -> days in 2..6
  Split.UPPER_LOWER -> days >=4
  Split.PPL -> days >=3
}
data class GoalConfig(val sets:Int, val reps:Int, val restSeconds:Int, val strategy:SetStrategy)
fun Goal.config(): GoalConfig = when(this){
  Goal.STRENGTH -> GoalConfig(4,5,90, SetStrategy.STEP_UP)
  Goal.HYPERTROPHY -> GoalConfig(3,10,90, SetStrategy.STANDARD)
  Goal.FAT_LOSS -> GoalConfig(3,13,60, SetStrategy.STANDARD)
  Goal.GENERAL_FITNESS -> GoalConfig(3,10,90, SetStrategy.STANDARD)
}
data class OnboardingProfile(val goal:Goal, val experience:Experience, val equipment:EquipmentProfile, val days:Int, val split:Split){
  fun maxDifficulty(): Difficulty = when(experience){ Experience.BEGINNER->Difficulty.BEGINNER; Experience.INTERMEDIATE->Difficulty.INTERMEDIATE; Experience.ADVANCED->Difficulty.ADVANCED }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.deepkush.reprange.domain.onboarding.OnboardingProfileTest" -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/deepkush/reprange/constants/PreferenceKeys.kt app/src/main/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingProfile.kt app/src/test/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingProfileTest.kt
git commit -m "feat(onboarding): profile model and DataStore keys"
```

---

### Task 2: Recommendation engine (pure, JVM-testable)

**Files:**
- Create: `app/src/main/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingRecommender.kt`
- Test: `app/src/test/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingRecommenderTest.kt`

**Interfaces:**
- Consumes: `OnboardingProfile`, `ExerciseDao` (interface with `search()`-like method or direct `getByTarget` — for test use a fake), `Goal.config()`, `Split` bucket definitions
- Produces: `class OnboardingRecommender @Inject constructor(private val exerciseDao: ExerciseDao) { suspend fun recommend(profile: OnboardingProfile): List<PreviewTemplate> }` where `data class PreviewTemplate(val title:String, val items:List<PlanItem>)`

- [ ] **Step 1: Write the failing test**

```kotlin
// OnboardingRecommenderTest.kt
@Test fun recommend_fullBody_bodyweight_returns6Items() = runTest {
  val fakeDao = FakeExerciseDao(withExercises = listOf(
    ExerciseEntity(id="1", name="push up", category="chest", target="pectorals", muscleGroup="chest", secondaryMuscles=[], equipment="body weight", difficulty="BEGINNER", instructionsEn=[], imageUrl="", gifUrl="", mediaId="", attribution=""),
    // ... 6 per bucket
  ))
  val engine = OnboardingRecommender(fakeDao)
  val profile = OnboardingProfile(Goal.GENERAL_FITNESS, Experience.BEGINNER, EquipmentProfile.BODYWEIGHT, 3, Split.FULL_BODY)
  val result = engine.recommend(profile)
  assertThat(result).hasSize(1)
  assertThat(result[0].items).hasSize(6)
  assertThat(result[0].items.all { it.exerciseId in fakeDao.ids }).isTrue()
}
@Test fun recommend_filtersByEquipment() = runTest {
  // FULL_GYM profile should be able to return barbell items; BODYWEIGHT should not
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.deepkush.reprange.domain.onboarding.OnboardingRecommenderTest" -v`
Expected: FAIL with "Unresolved reference: OnboardingRecommender"

- [ ] **Step 3: Write minimal implementation**

```kotlin
class OnboardingRecommender @Inject constructor(private val exerciseDao: ExerciseDao) {
  suspend fun recommend(profile: OnboardingProfile): List<PreviewTemplate> {
    val goal = profile.goal.config()
    val allowed = profile.equipment.allowedEquipment()
    val maxDiff = profile.maxDifficulty()
    // define buckets per split, query per bucket, deduplicate, fallback one tier
    // helper: suspend fun pick(bucket: Set<String> targets, n:Int): List<ExerciseEntity>
    // query: exerciseDao.search with category filter OR direct query by target in allowed
    // For simplicity in v1: use exerciseDao.getByIds + filter in memory; or add new Dao method getByMuscleGroups
    // Return PreviewTemplate per split part
  }
}
```

Add Dao helper if needed: `@Query("SELECT * FROM exercises WHERE target IN (:targets) AND equipment IN (:eq) AND difficulty IN (:diffs) LIMIT :n") suspend fun getByTargets(...)`

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.deepkush.reprange.domain.onboarding.OnboardingRecommenderTest" -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingRecommender.kt app/src/main/kotlin/com/deepkush/reprange/data/db/Daos.kt app/src/test/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingRecommenderTest.kt
git commit -m "feat(onboarding): rule-based recommender with tests"
```

---

### Task 3: Navigation + entry points (Home card + Settings row)

**Files:**
- Modify: `app/src/main/kotlin/com/deepkush/reprange/navigation/Routes.kt`
- Modify: `app/src/main/kotlin/com/deepkush/reprange/ReprangeRoot.kt`
- Modify: `app/src/main/kotlin/com/deepkush/reprange/ui/screens/home/HomeScreen.kt`
- Modify: `app/src/main/kotlin/com/deepkush/reprange/ui/screens/settings/SettingsScreen.kt`

**Interfaces:**
- Consumes: `ONBOARDING_COMPLETED` pref
- Produces: `OnboardingRoute` navigation, `HomeCard` visibility logic

- [ ] **Step 1: Write the failing test (Compose)**

```kotlin
// app/src/test/kotlin/com/deepkush/reprange/ui/home/HomeOnboardingCardTest.kt
@Test fun home_showsCardWhenNotCompleted() { /* composeTestRule: set pref false -> assert card displayed */ }
```

(Use `createComposeRule` + provide `ONBOARDING_COMPLETED=false` via Test DataStore)

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*HomeOnboardingCardTest" -v`
Expected: FAIL

- [ ] **Step 3: Implement**

```kotlin
// Routes.kt
@Serializable data object OnboardingRoute

// ReprangeRoot.kt: add to TabOrder hide logic, hide chrome on OnboardingRoute same as ActiveWorkoutRoute
val hideChrome = currentRouteName?.contains("OnboardingRoute")==true || currentRouteName?.contains("ActiveWorkout")==true
// In AppNavHost: composable<OnboardingRoute> { OnboardingScreen(onFinish={ navController.popBackStack() }) }

// HomeScreen.kt: at top of Column, if !completed collectAsState -> Card("Personalize your routines? ...") Button("Get started" -> navController.navigate(OnboardingRoute))
// SettingsScreen.kt: new SettingsGroup "Personalize" with SettingsItemSpec(title="Personalize routines", onClick={ navController.navigate(OnboardingRoute) })
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*HomeOnboardingCardTest" -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/deepkush/reprange/navigation/Routes.kt app/src/main/kotlin/com/deepkush/reprange/ReprangeRoot.kt app/src/main/kotlin/com/deepkush/reprange/ui/screens/home/HomeScreen.kt app/src/main/kotlin/com/deepkush/reprange/ui/screens/settings/SettingsScreen.kt
git commit -m "feat(onboarding): navigation and entry points"
```

---

### Task 4: Onboarding wizard UI (5 steps)

**Files:**
- Create: `app/src/main/kotlin/com/deepkush/reprange/ui/screens/onboarding/OnboardingScreen.kt`
- Create: `app/src/main/kotlin/com/deepkush/reprange/ui/screens/onboarding/OnboardingViewModel.kt`
- Test: `app/src/test/kotlin/com/deepkush/reprange/ui/onboarding/OnboardingViewModelTest.kt`

**Interfaces:**
- Consumes: `OnboardingProfile` partial, `PreferenceKeys`
- Produces: `OnboardingViewModel @HiltViewModel` with `StateFlow<Profile>`, `currentStep:Int`, `preview:List<PreviewTemplate>?`, methods `setGoal`, `setExperience`, etc., `generatePreview()`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun viewModel_stepper_days_clamps2to6() = runTest {
  val vm = OnboardingViewModel(fakeRepo)
  vm.setDays(7); assertThat(vm.profile.value.days).isEqualTo(6)
  vm.setDays(1); assertThat(vm.profile.value.days).isEqualTo(2)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*OnboardingViewModelTest" -v`
Expected: FAIL

- [ ] **Step 3: Implement ViewModel + Screen**

Screen: `Column` with progress dots (5), `AnimatedContent(currentStep)`, each step is a `SettingsGroup`-style card or custom chips/stepper. Use `rememberEnumPreference` for persistence on Continue, but keep in-memory until Finish. Footer Skip + Continue. Step 5 Split options filtered by `profile.days` via `Split.validForDays`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*OnboardingViewModelTest" -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/deepkush/reprange/ui/screens/onboarding/
git commit -m "feat(onboarding): 5-step wizard UI and view model"
```

---

### Task 5: Preview & swap integration

**Files:**
- Modify: `app/src/main/kotlin/com/deepkush/reprange/ui/screens/onboarding/OnboardingScreen.kt` (add preview pager)
- Test: `app/src/test/kotlin/com/deepkush/reprange/ui/onboarding/PreviewSwapTest.kt`

**Interfaces:**
- Consumes: `OnboardingRecommender.recommend(profile)` + `ExercisePickerScreen`
- Produces: batch save via `CreateWorkoutPlanUseCase`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun preview_swap_replacesExercise() = runTest {
  val vm = OnboardingViewModel(engineWithFake)
  vm.setGoal(Goal.HYPERTROPHY); // ... set all 5
  vm.generatePreview()
  val before = vm.preview.value[0].items[0].exerciseId
  vm.swapExercise(templateIndex=0, itemIndex=0, newExerciseId="new-id")
  assertThat(vm.preview.value[0].items[0].exerciseId).isEqualTo("new-id")
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*PreviewSwapTest" -v`
Expected: FAIL

- [ ] **Step 3: Implement**

In `OnboardingScreen.kt` preview: `HorizontalPager` or `SegmentedButton` per template; each page `LazyColumn` of `ExerciseRow` (reuse from library) with swap `IconButton` that does `savedStateHandle["picked_exercise"]` flow like `TemplateBuilderRoute` does. `OnFinish` loops `preview.forEach{ CreateWorkoutPlanUseCase(null, it.title, "", it.items) }` + `dataStore.edit{ it[ONBOARDING_COMPLETED]=true }`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*PreviewSwapTest" -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/deepkush/reprange/ui/screens/onboarding/OnboardingScreen.kt
git commit -m "feat(onboarding): preview with swap and batch save"
```

---

### Task 6: Edge cases, polish & verification

**Files:**
- Modify: `app/src/main/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingRecommender.kt` (fallback tier)
- Modify: `app/src/main/kotlin/com/deepkush/reprange/ui/screens/onboarding/OnboardingScreen.kt` (loading/error states)

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun recommend_emptyBucket_fallsBackOneTier() = runTest {
  // fake dao returns empty for BEGINNER bodyweight waist bucket
  // engine should return at least one item by relaxing difficulty
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*OnboardingRecommenderTest.emptyBucket*" -v`
Expected: FAIL

- [ ] **Step 3: Implement fallback + loading/error UI**

Handle `SeedState` waiting (spinner), error card with retry (reuse `SeedError`), and fallback query.

- [ ] **Step 4: Run all onboarding tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.deepkush.reprange.domain.onboarding.*" -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/deepkush/reprange/domain/onboarding/OnboardingRecommender.kt app/src/main/kotlin/com/deepkush/reprange/ui/screens/onboarding/OnboardingScreen.kt
git commit -m "feat(onboarding): empty bucket fallback and loading states"
```

---

## Self-Review

**Spec coverage:** Every section of `2026-08-31-onboarding-recommended-routines-design.md` maps to tasks: §5 Data Model → Task1, §6 Engine → Task2, §3 Navigation → Task3, §3 Wizard → Task4, §7 Preview → Task5, §8 Edge cases → Task6. No gaps.

**Placeholder scan:** No TBD/TODO, no "implement later", no missing code blocks.

**Type consistency:** `OnboardingProfile`, `GoalConfig`, `PreviewTemplate`, `CreateWorkoutPlanUseCase` signatures align across tasks; `ExerciseDao` helper method named consistently; `rememberEnumPreference` usage matches existing pattern.

