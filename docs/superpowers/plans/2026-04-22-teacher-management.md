# План впровадження модуля «Керування викладачами» (ASMS V3)

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Реалізація інтерфейсу для керування персоналом (CRUD), включаючи облік кафедр, посад та лімітів навантаження.

**Архітектура:** Hilla (React) фронтенд з `@BrowserCallable` Java-ендпоінтами. Використання Tailwind CSS та Aura Theme.

**Технологічний стек:** Java 21, Spring Boot 4, Vaadin 25.1.2 (Hilla), React 19, Tailwind CSS.

---

### Завдання 1: Бекенд — DTO та Endpoint для викладачів

**Файли:**
- Оновити: `src/main/java/com/sergofoox/domain/ui/dto/TeacherDTO.java`
- Створити: `src/main/java/com/sergofoox/domain/teacher/TeacherEndpoint.java`

- [ ] **Крок 1: Оновлення TeacherDTO**
  Додати поле `weeklyHourLimit` для синхронізації з сутністю.

- [ ] **Крок 2: Створення TeacherEndpoint**
  Реалізувати методи:
  - `List<TeacherDTO> getAllTeachers()`
  - `void saveTeacher(TeacherDTO teacher)`
  - `void deleteTeacher(Long id)`
  - Методи захистити `@RolesAllowed("DISPATCHER")`.

---

### Завдання 2: Фронтенд — Список викладачів з Grid та Пошуком

**Файли:**
- Модифікувати: `src/main/frontend/views/teachers.tsx`

- [ ] **Крок 1: Реалізація Grid для викладачів**
  Колонки: ПІБ, Кафедра, Посада, Ліміт годин/тиждень.

- [ ] **Крок 2: Панель інструментів (Toolbar)**
  Поле пошуку за ПІБ/кафедрою та кнопка «Додати викладача».

---

### Завдання 3: Фронтенд — Діалог створення та редагування

**Файли:**
- Створити: `src/main/frontend/components/TeacherDialog.tsx`
- Модифікувати: `src/main/frontend/views/teachers.tsx` (інтеграція)

- [ ] **Крок 1: Створення компонента TeacherDialog**
  Поля: `TextField` (ПІБ, кафедра), `ComboBox` (посада), `IntegerField` (ліміт годин).

- [ ] **Крок 2: Зв'язування з бекендом**
  Виклик `TeacherEndpoint.saveTeacher()` та оновлення списку.

---

### Завдання 4: Фіналізація та Тестування

- [ ] **Крок 1: Стилізація Tailwind**
  Забезпечення візуальної ідентичності з іншими розділами.

- [ ] **Крок 2: Перевірка валідації**
  Додавання обробки помилок та підтвердження видалення викладача.
