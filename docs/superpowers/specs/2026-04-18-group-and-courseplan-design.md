# Group and CoursePlan Design Spec

## Goal
Implement the `Group` (Група) and `CoursePlan` (Навчальний план) entities to define student groups and their academic requirements for the scheduling system.

## Architecture
- **Layer:** Domain/Infrastructure (JPA Entities)
- **Package:** `com.sergofoox.domain.group` and `com.sergofoox.domain.plan`
- **Pattern:** standard Java POJOs with JPA annotations and Bean Validation.

## Data Models

### Group
| Field | Type | Description | JPA Mapping |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Primary Key | `@Id`, `@GeneratedValue` |
| `name` | `String` | Group name (e.g., "KB-41") | `@NotBlank`, `@Column(nullable = false)` |
| `size` | `Integer` | Number of students | `@NotNull`, `@Min(1)`, `@Column(nullable = false)` |
| `course` | `Integer` | Study year (1–4) | `@NotNull`, `@Min(1)`, `@Max(4)`, `@Column(nullable = false)` |
| `department` | `String` | Department name | `@NotBlank`, `@Column(nullable = false)` |

### RoomType (Enum)
- `LECTURE_HALL`
- `LABORATORY`
- `COMPUTER_CLASS`
- `GENERAL_CLASSROOM`

### CoursePlan
| Field | Type | Description | JPA Mapping |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Primary Key | `@Id`, `@GeneratedValue` |
| `subject` | `Subject` | Linked subject | `@ManyToOne(fetch = LAZY)`, `@NotNull`, `@JoinColumn(nullable = false)` |
| `group` | `Group` | Linked group | `@ManyToOne(fetch = LAZY)`, `@NotNull`, `@JoinColumn(nullable = false)` |
| `totalHours` | `Integer` | Total hours for the subject | `@NotNull`, `@Min(0)` |
| `lectureHours` | `Integer` | Hours for lectures | `@NotNull`, `@Min(0)` |
| `practiceHours` | `Integer` | Hours for practice | `@NotNull`, `@Min(0)` |
| `labHours` | `Integer` | Hours for labs | `@NotNull`, `@Min(0)` |
| `lectureSessionsPerWeek` | `Integer` | Distribution: lectures/week | `@NotNull`, `@Min(0)` |
| `practiceSessionsPerWeek` | `Integer` | Distribution: practice/week | `@NotNull`, `@Min(0)` |
| `labSessionsPerWeek` | `Integer` | Distribution: labs/week | `@NotNull`, `@Min(0)` |
| `requiredRoomType` | `RoomType` | Constraint: required room | `@Enumerated(STRING)`, `@NotNull` |

## Requirements
1. **Standard Boilerplate:** No-args/All-args constructors, Getters/Setters, Equals/HashCode/ToString.
2. **Equals/HashCode:** Use business keys for JPA safety (name/course/department for Group; subject/group for CoursePlan).
3. **Validation:** Use `jakarta.validation.constraints` for all mandatory and range-limited fields.
4. **Fetch Strategy:** Use `FetchType.LAZY` for all `@ManyToOne` relationships.
