# System Patterns — ASMS V3

## Architecture Style

### Clean Architecture + Domain-Driven Design (DDD)

The system follows:
- Separation of concerns
- Independent domain logic
- Explicit business rules

---

## Layers

### 1. Domain Layer
- Entities (Teacher, Subject, Lesson, Schedule)
- Business rules
- Constraints

### 2. Application Layer
- Use cases (GenerateSchedule, ReplaceTeacher)
- Orchestration of domain logic

### 3. Infrastructure Layer
- Database
- External integrations
- Timefold solver integration

### 4. Presentation Layer
- Vaadin UI
- Dashboards

---

## Architectural Patterns

### Hexagonal Architecture (Ports & Adapters)
- Isolates domain from infrastructure
- Improves testability

---

## UI Approach

### Vaadin Flow
- Server-side UI in Java
- Component-based structure
- Reactive updates (optional via signals/events)

---

## Optimization Engine

### Timefold Solver
- Constraint-based optimization
- Supports Hard and Soft constraints

---

## Key Patterns

- Partial CQRS
- Factory (schedule creation)
- Strategy (constraint logic)
- Builder (complex object construction)

---

## Principles

- Single Source of Truth (Memory Bank)
- Explicit business logic
- Minimal side effects