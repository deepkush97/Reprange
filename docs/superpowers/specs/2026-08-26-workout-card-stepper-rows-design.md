# Workout Card Redesign — Stepper Rows + Planned Sets

Date: 2026-08-26
Status: Approved (mix of Approach A + B)

## Problem
The active-workout card used two large `OutlinedTextField`s (kg/reps) that dominated
the card, forced the soft keyboard for every set, and gave no sense of plan vs. done.

## Design

### Row model
Each set is one compact row inside the exercise card:

```
[✓ 32dp] [#n] [ weight stepper ] [ reps stepper ] [type icon]
```

- **StepperValue**: tappable value (opens compact `TextFieldDialog` for fine input)
  with − / + buttons (±2.5 kg, ±1 rep). No full-size text fields anywhere.
- **Two row kinds**:
  - *Planned* (hollow ✓, medium-emphasis values): created from the template's
    `setCount` / targets when the session started from a template; otherwise
    created by "Add set" (duplicates last row's values).
  - *Logged* (filled ✓, solid values): backed by a `CompletedSetEntity`;
    swipe-to-delete (existing behavior).
- **Set type per row**: tapping the row's type icon cycles
  Normal → Warmup → Drop set → Failure (icons: dumbbell / flame / trend-down / bolt).
  Logged rows keep the type they were logged with. The old chip row is removed.
- **Prefill**: template targets (`targetWeightKg`/`targetReps`) when present,
  else last-session suggestion; `STEP_UP` strategy escalates +2.5 kg per row index.
- **Add set**: appends a planned row duplicating the last row (logged or planned).

### Data changes
- `SessionExerciseEntity` + `planned_sets`, `target_weight_kg`, `target_reps`
  (nullable). DB version 1 → 2 (destructive fallback already enabled).
- `StartWorkoutSessionUseCase` copies template `setCount`/targets into the
  session entries.

### Unchanged
Rest timer auto-start, PR flash + haptics, volume totals, Next-up, discard/finish
dialogs, per-row swipe delete, `LogCompletedSetUseCase` (per-set DB write on ✓).

## Testing
Manual on emulator: blank-workout flow (add set → stepper ± → cycle type → ✓),
template flow (planned rows appear with targets), swipe delete, rest timer trigger.
