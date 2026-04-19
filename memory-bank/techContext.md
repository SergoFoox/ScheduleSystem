# Tech Context — ASMS V3

## Core Stack

### Backend
- Java 21
- Spring Boot 4.0.5 (Starter Parent)

### Frontend
- Vaadin 25.1.2
- Aura theme
- React integration (as per package.json)

### Optimization Engine
- Timefold 1.33.0

---

## Architecture
- Clean Architecture + DDD
- Domain-by-feature packaging (e.g., com.sergofoox.domain.teacher)
- Monolithic structure

---

## Database
- H2 Database (In-memory for development/test)
- PostgreSQL (Production target)

---

## Core Components

### Scheduling Engine
- Timefold Solver (PlanningEntity, PlanningVariable, PlanningSolution)
- ConstraintProvider (Java Streams API)

### UI Layer
- Vaadin Flow Views
- Dashboards with React components

### Data Layer
- JPA / Hibernate
- Bean Validation (jakarta.validation)

---

## Export Formats
- HTML
- PDF

---

## Deployment (initial)
- Docker
- Maven Wrapper (mvnw)

---

## Development Tools
- Maven
- IntelliJ IDEA / VS Code
- Gemini CLI (AI Assistant)

---

## Key Technical Requirements
- Efficient constraint solving (Hard/Soft constraints)
- Fast schedule recalculation
- High extensibility for new constraint types