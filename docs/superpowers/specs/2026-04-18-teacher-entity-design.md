# Teacher Entity Design Spec

## Goal
Implement the `Teacher` (Викладач) domain entity as a JPA-managed persistence object to store information about academic staff.

## Architecture
- **Layer:** Domain/Infrastructure (JPA Entity)
- **Package:** `com.sergofoox.domain.teacher`
- **Pattern:** standard Java POJO with JPA annotations.

## Data Model
| Field | Type | Description | JPA Mapping |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Primary Key | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `fullName` | `String` | ПІБ (Full Name) | `@Column(nullable = false)` |
| `department` | `String` | Кафедра (Department) | `@Column(nullable = false)` |
| `positionType` | `String` | Тип ставки (Position Type) | `@Column(nullable = false)` |

## Requirements
1. **No-args Constructor:** Required by JPA.
2. **All-args Constructor:** For convenient instantiation.
3. **Getters/Setters:** Standard boilerplate for all fields.
4. **Equals/HashCode:** Based on `id` (or all fields if `id` is null) for consistent behavior in collections and Timefold.
5. **ToString:** For debugging purposes.

## Validation
- `fullName`, `department`, and `positionType` should not be null in the database.
