# Специфікація дизайну сутності Teacher

## Мета
Впровадження доменної сутності `Teacher` (Викладач) як керованого JPA об'єкта стійкості для зберігання інформації про викладацький склад.

## Архітектура
- **Рівень:** Домен/Інфраструктура (JPA-сутність)
- **Пакет:** `com.sergofoox.domain.teacher`
- **Шаблон:** стандартний Java POJO з анотаціями JPA.

## Модель даних
| Поле | Тип | Опис | Мапінг JPA |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Первинний ключ | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `fullName` | `String` | ПІБ (Full Name) | `@Column(nullable = false)` |
| `department` | `String` | Кафедра (Department) | `@Column(nullable = false)` |
| `positionType` | `String` | Тип ставки (Position Type) | `@Column(nullable = false)` |

## Вимоги
1. **Конструктор без аргументів:** Необхідний для JPA.
2. **Конструктор з усіма аргументами:** Для зручного створення екземплярів.
3. **Геттери/Сеттери:** Стандартний шаблонний код для всіх полів.
4. **Equals/HashCode:** На основі `id` (або всіх полів, якщо `id` дорівнює null) для узгодженої поведінки в колекціях та Timefold.
5. **ToString:** Для цілей налагодження.

## Валідація
- `fullName`, `department` та `positionType` не повинні бути null у базі даних.
