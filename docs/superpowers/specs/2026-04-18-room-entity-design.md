# Специфікація дизайну сутності Room

## Мета
Впровадження доменної сутності `Room` (Аудиторія) для зберігання інформації про фізичні навчальні приміщення, їхню місткість, обладнання та місцезнаходження.

## Архітектура
- **Рівень:** Домен/Інфраструктура (JPA-сутність)
- **Пакет:** `com.sergofoox.domain.room`
- **Шаблон:** стандартний Java POJO з анотаціями JPA та Bean Validation.

## Модель даних
| Поле | Тип | Опис | Мапінг JPA |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | Первинний ключ | `@Id`, `@GeneratedValue` |
| `name` | `String` | Назва/номер аудиторії (напр., "101") | `@NotBlank`, `@Column(nullable = false)` |
| `capacity` | `Integer` | Місткість студентів | `@NotNull`, `@Min(1)`, `@Column(nullable = false)` |
| `building` | `String` | Будівля/Корпус (напр., "Головний") | `@NotBlank`, `@Column(nullable = false)` |
| `equipment` | `String` | Список обладнання | `@Column` |
| `type` | `RoomType` | Категорія (LECTURE_HALL тощо) | `@Enumerated(STRING)`, `@NotNull` |

## Вимоги
1. **Стандартний шаблонний код:** Конструктори без аргументів/з усіма аргументами, геттери/сеттери, Equals/HashCode/ToString.
2. **Equals/HashCode:** Використовуйте бізнес-ключі для безпеки JPA (`name` та `building`).
3. **Валідація:** Використовуйте `jakarta.validation.constraints` для всіх обов'язкових полів та полів з обмеженням діапазону.
4. **Інтеграція:** Повторне використання перерахування `com.sergofoox.domain.plan.RoomType`.
