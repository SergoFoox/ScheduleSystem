# Progress — ASMS V3

## Current Status
🟢 Domain Model Completed & Planning Entities Defined

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
- [x] Domain model standardization (JPA proxy safety, Bean Validation, Timefold-safe toString)
- [x] **100% Test Coverage for Domain Layer** (26/26 tests passing)

---

## In Progress

- [ ] Timefold Planning Solution (Schedule class)
- [ ] ConstraintProvider definition (Hard/Soft constraints)

---

## Next Steps

1. Create `Schedule` class as the Timefold **@PlanningSolution**
2. Define first Hard Constraint (no teacher/group/room overlaps) in `ScheduleConstraintProvider`
3. Implement basic Vaadin dashboard for schedule visualization

---

## Risks

- High complexity of constraint logic
- Timefold performance tuning
- UI complexity in Vaadin

---

## Notes

- Timefold chosen over OptaPlanner (version 1.33.0)
- Domain entities use stable business keys for equals/hashCode
- Project follows clean architecture with domain-by-feature packaging
- Timeslot includes `weekParity` for alternating week scheduling (odd/even)
