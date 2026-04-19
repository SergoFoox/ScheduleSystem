# Decision Log — ASMS V3

## [2026-04-18] Optimization Engine Selection

### Decision
Use Timefold Solver 1.33.0 instead of OptaPlanner.

### Reasoning
- More active development and modern API.
- Better performance for high-concurrency scheduling.
- Improved Java 21 support.

---

## [2026-04-18] UI Framework Selection

### Decision
Use Vaadin Flow 25.1.2 with React integration.

### Reasoning
- Full-stack Java logic with modern React frontend flexibility.
- Component-based structure for complex dashboards.
- Native Spring Boot integration.

---

## [2026-04-19] Domain Model Design

### Decision
Adopt "Domain-by-feature" packaging and stable business keys for equals/hashCode.

### Reasoning
- Prevents N+1 and Proxy issues in JPA/Timefold.
- Improves modularity and makes the code more testable.
- Ensures stability during Solver iterations.

---

## [2026-04-19] Scheduling Cycles Implementation

### Decision
Implement `Periodicity` (WEEKLY, ODD_WEEKS, EVEN_WEEKS) at the `Timeslot` and `CoursePlan` level.

### Reasoning
- Fully satisfies requirement 3.1 for "Numerator/Denominator" support.
- Allows fine-grained control over alternating week schedules.