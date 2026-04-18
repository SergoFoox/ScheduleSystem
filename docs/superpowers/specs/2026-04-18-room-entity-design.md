# Room Entity Design Spec

## Goal
Implement the `Room` (Аудиторія) domain entity to store information about physical classrooms, their capacity, equipment, and location.

## Architecture
- **Layer:** Domain/Infrastructure (JPA Entity)
- **Package:** `com.sergofoox.domain.room`
- **Pattern:** standard Java POJO with JPA annotations and Bean Validation.

## Data Model
| Field | Type | Description | JPA Mapping |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Primary Key | `@Id`, `@GeneratedValue` |
| `name` | `String` | Room name/number (e.g., "101") | `@NotBlank`, `@Column(nullable = false)` |
| `capacity` | `Integer` | Student capacity | `@NotNull`, `@Min(1)`, `@Column(nullable = false)` |
| `building` | `String` | Building/Corps (e.g., "Main") | `@NotBlank`, `@Column(nullable = false)` |
| `equipment` | `String` | List of equipment | `@Column` |
| `type` | `RoomType` | Category (LECTURE_HALL, etc.) | `@Enumerated(STRING)`, `@NotNull` |

## Requirements
1. **Standard Boilerplate:** No-args/All-args constructors, Getters/Setters, Equals/HashCode/ToString.
2. **Equals/HashCode:** Use business keys for JPA safety (`name` and `building`).
3. **Validation:** Use `jakarta.validation.constraints` for all mandatory and range-limited fields.
4. **Integration:** Reuse `com.sergofoox.domain.plan.RoomType` enum.
