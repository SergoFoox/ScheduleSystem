# Специфікація дизайну Group та CoursePlan

## Мета
Впровадження сутностей `Group` (Група) та `CoursePlan` (Навчальний план) для визначення студентських груп та їхніх академічних вимог для системи планування розкладу.

## Архітектура
- **Рівень:** Домен/Інфраструктура (JPA-сутності)
- **Пакет:** `com.sergofoox.domain.group` та `com.sergofoox.domain.plan`
- **Шаблон:** стандартні Java POJO з анотаціями JPA та Bean Validation.

## Моделі даних

### Group (Група)
| Поле | Тип | Опис | Мапінг JPA |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Первинний ключ | `@Id`, `@GeneratedValue` |
| `name` | `String` | Назва групи (напр., "КБ-41") | `@NotBlank`, `@Column(nullable = false)` |
| `size` | `Integer` | Кількість студентів | `@NotNull`, `@Min(1)`, `@Column(nullable = false)` |
| `course` | `Integer` | Рік навчання (1–4) | `@NotNull`, `@Min(1)`, `@Max(4)`, `@Column(nullable = false)` |
| `department` | `String` | Назва кафедри/факультету | `@NotBlank`, `@Column(nullable = false)` |

### RoomType (Enum, Тип приміщення)
- `LECTURE_HALL` (Лекційна аудиторія)
- `LABORATORY` (Лабораторія)
- `COMPUTER_CLASS` (Комп'ютерний клас)
- `GENERAL_CLASSROOM` (Загальна аудиторія)

### CoursePlan (Навчальний план)
| Поле | Тип | Опис | Мапінг JPA |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Первинний ключ | `@Id`, `@GeneratedValue` |
| `subject` | `Subject` | Пов'язаний предмет | `@ManyToOne(fetch = LAZY)`, `@NotNull`, `@JoinColumn(nullable = false)` |
| `group` | `Group` | Пов'язана група | `@ManyToOne(fetch = LAZY)`, `@NotNull`, `@JoinColumn(nullable = false)` |
| `totalHours` | `Integer` | Загальна кількість годин | `@NotNull`, `@Min(0)` |
| `lectureHours` | `Integer` | Години лекцій | `@NotNull`, `@Min(0)` |
| `practiceHours` | `Integer` | Години практичних занять | `@NotNull`, `@Min(0)` |
| `labHours` | `Integer` | Години лабораторних занять | `@NotNull`, `@Min(0)` |
| `lectureSessionsPerWeek` | `Integer` | Розподіл: лекцій на тиждень | `@NotNull`, `@Min(0)` |
| `practiceSessionsPerWeek` | `Integer` | Розподіл: практик на тиждень | `@NotNull`, `@Min(0)` |
| `labSessionsPerWeek` | `Integer` | Розподіл: лаб на тиждень | `@NotNull`, `@Min(0)` |
| `requiredRoomType` | `RoomType` | Обмеження: необхідний тип аудиторії | `@Enumerated(STRING)`, `@NotNull` |

## Вимоги
1. **Стандартний шаблонний код:** Конструктори без аргументів/з усіма аргументами, геттери/сеттери, Equals/HashCode/ToString.
2. **Equals/HashCode:** Використовуйте бізнес-ключі для безпеки JPA (name/course/department для Group; subject/group для CoursePlan).
3. **Валідація:** Використовуйте `jakarta.validation.constraints` для всіх обов'язкових полів та полів з обмеженням діапазону.
4. **Стратегія вибірки:** Використовуйте `FetchType.LAZY` для всіх зв'язків `@ManyToOne`.
