# Progress — ASMS V3

## Current Status
🟢 Timefold Constraints Finalized & Verified

---

## Completed

- [x] Memory Bank structure created
- [x] Core context documents defined
- [x] Teacher domain entity (standardized with validation, workload limits)
- [x] Subject domain entity and LessonType enum
- [x] TeacherCompetenceMatrix junction entity and refined Priority enum (PRIMARY, SECONDARY, SUBSTITUTE)
- [x] Group domain entity (student_group mapping)
- [x] CoursePlan domain entity (hours consistency validation, periodicity support, executed hours tracking)
- [x] Room domain entity and RoomType enum
- [x] **Timeslot** domain entity (DayOfWeek, LocalTime, weekParity/periodicity support)
- [x] **Lesson** domain entity (Timefold **@PlanningEntity** with **@PlanningVariable** for timeslot and room)
- [x] **Schedule** class as the Timefold **@PlanningSolution**
- [x] **ScheduleConstraintProvider** with **All Required Constraints**:
    - **Hard**: Teacher, Group, Room conflicts (with weekParity support)
    - **Hard**: Room Capacity vs Group Size
    - **Soft**: Minimization of "windows" (gaps) for Groups and Teachers
    - **Soft**: Load balancing (preventing more than 4 lessons per day)
    - **Soft**: Room stability for Teachers (prefer same room during the day)
- [x] **SolverConfiguration** (Java API) with **FIRST_FIT** and **TABU_SEARCH** strategies
- [x] Domain model standardization (JPA proxy safety, Bean Validation, Timefold-safe toString)
- [x] **100% Test Coverage for Domain & Solver** (27 tests passing, including Solver Integration)

---

## In Progress

- [ ] JPA Repositories for all domain entities
- [ ] Service layer for schedule generation orchestration

---

## Next Steps

1. Create JPA Repositories for CRUD operations on domain entities.
2. Implement `ScheduleService` to load data into the `Schedule` object and trigger solving.
3. Implement basic Vaadin dashboard for schedule visualization and management.

---

## Risks

- High complexity of advanced constraint logic (e.g., travel time between buildings)
- Performance tuning for large-scale datasets
- Vaadin UI state management during asynchronous solving

---

## Notes

- Timefold chosen over OptaPlanner (version 1.33.0)
- Solver configured with 2-minute termination limit for initial testing
- Constraint Streams API used for better readability and performance
- `ScheduleSolverTest` verifies that the solver can successfully resolve teacher overlaps and apply soft constraints
