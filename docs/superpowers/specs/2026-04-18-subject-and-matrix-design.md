# Subject and TeacherCompetenceMatrix Design Spec

## Goal
Implement the `Subject` (Предмет) entity and the `TeacherCompetenceMatrix` (Матриця компетентності викладачів) to define which teachers can teach which subjects, with what priority and for which lesson types.

## Architecture
- **Layer:** Domain/Infrastructure (JPA Entities)
- **Package:** `com.sergofoox.domain.subject` and `com.sergofoox.domain.competence`
- **Pattern:** standard Java POJOs with JPA annotations.

## Data Models

### Subject
| Field | Type | Description | JPA Mapping |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Primary Key | `@Id`, `@GeneratedValue` |
| `name` | `String` | Full name of the subject | `@Column(nullable = false)` |
| `abbreviation` | `String` | Short name (e.g., "Math") | `@Column(nullable = false)` |

### LessonType (Enum)
- `LECTURE`
- `PRACTICE`
- `LABORATORY`

### Priority (Enum)
- `HIGH`
- `MEDIUM`
- `LOW`

### TeacherCompetenceMatrix
| Field | Type | Description | JPA Mapping |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Primary Key | `@Id`, `@GeneratedValue` |
| `teacher` | `Teacher` | The teacher | `@ManyToOne`, `@JoinColumn(nullable = false)` |
| `subject` | `Subject` | The subject | `@ManyToOne`, `@JoinColumn(nullable = false)` |
| `lessonType` | `LessonType` | Type of lesson | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)` |
| `priority` | `Priority` | Competence priority | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)` |

## Requirements
1. **Standard Boilerplate:** No-args/All-args constructors, Getters/Setters, Equals/HashCode/ToString.
2. **Package Structure:**
   - `com.sergofoox.domain.subject.Subject`
   - `com.sergofoox.domain.subject.LessonType`
   - `com.sergofoox.domain.competence.Priority`
   - `com.sergofoox.domain.competence.TeacherCompetenceMatrix`
3. **Validation:** All fields are mandatory (non-nullable).
