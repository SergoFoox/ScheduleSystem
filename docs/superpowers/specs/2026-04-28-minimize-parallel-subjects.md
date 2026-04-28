# Design Document: Minimize Parallel Subject Lessons

## Goal
The goal is to prevent the solver from scheduling the same subject (e.g., "Ukrainian Language") for different groups at the same time slot, even if the teachers and rooms are different. This is requested to maintain a more balanced schedule across groups.

## Approach: Soft Constraint
We will implement this as a **Soft Constraint** to allow the solver flexibility. If the schedule is too crowded, it can still place these lessons together, but it will prefer to separate them to avoid the penalty.

## Architecture & Implementation

### 1. `ScheduleConstraintProvider.java`
Add a new constraint method `minimizeParallelSubjectLessons`.

- **Constraint Type**: Soft Penalty.
- **Joiners**: 
    - `Lesson::getSubject`
    - `Timeslot::getDayOfWeek`
    - `Timeslot::getLessonNumber`
- **Filter**: 
    - Lessons must overlap in weeks (weekly/odd/even).
    - Lessons must be for different groups (to avoid conflict with existing hard group constraints, although those already handle this).
- **Penalty**: `HardSoftScore.ofSoft(50)`.

### 2. Validation
- Run `ScheduleSolverTest` to ensure no regressions.
- Add a new test case to `ScheduleSolverTest` that specifically checks if the solver prefers to separate same-subject lessons when possible.

## Success Criteria
- The solver should actively try to avoid placing the same subject in the same timeslot for different groups.
- The constraint must not prevent the solver from finding a feasible (0 hard conflicts) solution.
