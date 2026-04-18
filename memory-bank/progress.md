# Progress — ASMS V3

## Current Status
🟢 Core Domain Entities Completed

---

## Completed

- [x] Memory Bank structure created
- [x] Core context documents defined
- [x] Teacher domain entity (standardized with validation and business key)
- [x] Subject domain entity and LessonType enum
- [x] TeacherCompetenceMatrix junction entity and Priority enum
- [x] Group domain entity
- [x] CoursePlan domain entity with hours consistency validation
- [x] Room domain entity and RoomType enum
- [x] Domain model standardization (JPA proxy safety, Bean Validation)

---

## In Progress

- [ ] Timefold Solver configuration
- [ ] Initial scheduling constraints definition

---

## Next Steps

1. Set up Timefold Solver and Solution classes
2. Define first Hard Constraint (no teacher/group/room overlaps)
3. Implement basic Vaadin dashboard for schedule visualization

---

## Risks

- High complexity of constraint logic
- Timefold performance tuning
- UI complexity in Vaadin

---

## Notes

- Timefold chosen over OptaPlanner
- Domain entities use stable business keys for equals/hashCode
- Project follows clean architecture with domain-by-feature packaging
