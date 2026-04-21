# Специфікація дизайну Subject та TeacherCompetenceMatrix

## Мета
Впровадження сутності `Subject` (Предмет) та сутності `TeacherCompetenceMatrix` (Матриця компетентності викладачів) для визначення того, які викладачі можуть викладати які предмети, з яким пріоритетом та для яких типів занять.

## Архітектура
- **Рівень:** Домен/Інфраструктура (JPA-сутності)
- **Пакет:** `com.sergofoox.domain.subject` та `com.sergofoox.domain.competence`
- **Шаблон:** стандартні Java POJO з анотаціями JPA.

## Моделі даних

### Subject (Предмет)
| Поле | Тип | Опис | Мапінг JPA |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Первинний ключ | `@Id`, `@GeneratedValue` |
| `name` | `String` | Повна назва предмета | `@Column(nullable = false)` |
| `abbreviation` | `String` | Скорочена назва (напр., "Мат") | `@Column(nullable = false)` |

### LessonType (Enum, Тип заняття)
- `LECTURE` (Лекція)
- `PRACTICE` (Практика)
- `LABORATORY` (Лабораторна)

### Priority (Enum, Пріоритет)
- `HIGH` (Високий)
- `MEDIUM` (Середній)
- `LOW` (Низький)

### TeacherCompetenceMatrix (Матриця компетентності викладачів)
| Поле | Тип | Опис | Мапінг JPA |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Первинний ключ | `@Id`, `@GeneratedValue` |
| `teacher` | `Teacher` | Викладач | `@ManyToOne`, `@JoinColumn(nullable = false)` |
| `subject` | `Subject` | Предмет | `@ManyToOne`, `@JoinColumn(nullable = false)` |
| `lessonType` | `LessonType` | Тип заняття | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)` |
| `priority` | `Priority` | Пріоритет компетенції | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)` |

## Вимоги
1. **Стандартний шаблонний код:** Конструктори без аргументів/з усіма аргументами, геттери/сеттери, Equals/HashCode/ToString.
2. **Структура пакетів:**
   - `com.sergofoox.domain.subject.Subject`
   - `com.sergofoox.domain.subject.LessonType`
   - `com.sergofoox.domain.competence.Priority`
   - `com.sergofoox.domain.competence.TeacherCompetenceMatrix`
3. **Валідація:** Усі поля є обов'язковими (не можуть бути null).
