# План впровадження модуля «Керування групами» (ASMS V3)

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Реалізація інтерфейсу для керування навчальними групами (CRUD), включаючи облік чисельності студентів, курсу та кафедри.

**Архітектура:** Hilla (React) фронтенд з `@BrowserCallable` Java-ендпоінтами. Використання Tailwind CSS та Aura Theme.

**Технологічний стек:** Java 21, Spring Boot 4, Vaadin 25.1.2 (Hilla), React 19, Tailwind CSS.

---

### Завдання 1: Бекенд — DTO та Endpoint для груп

**Файли:**
- Створити/Оновити: `src/main/java/com/sergofoox/domain/ui/dto/GroupDTO.java`
- Створити: `src/main/java/com/sergofoox/domain/group/GroupEndpoint.java`

- [ ] **Крок 1: Перевірка GroupDTO**
  DTO повинен містити: `id`, `name`, `size`, `course`, `department`.

- [ ] **Крок 2: Створення GroupEndpoint**
  Реалізувати методи:
  - `List<GroupDTO> getAllGroups()`
  - `void saveGroup(GroupDTO group)`
  - `void deleteGroup(Long id)`
  - Методи мають бути захищені `@RolesAllowed("DISPATCHER")`.

---

### Завдання 2: Фронтенд — Список груп з Grid та Пошуком

**Файли:**
- Модифікувати: `src/main/frontend/views/groups.tsx`

- [ ] **Крок 1: Реалізація Grid для груп**
  Колонки: Назва групи, Курс, Кількість студентів, Кафедра.

- [ ] **Крок 2: Панель інструментів (Toolbar)**
  Поле пошуку за назвою/кафедрою та кнопка «Додати групу».

---

### Завдання 3: Фронтенд — Діалог створення та редагування

**Файли:**
- Створити: `src/main/frontend/components/GroupDialog.tsx`
- Модифікувати: `src/main/frontend/views/groups.tsx` (інтеграція)

- [ ] **Крок 1: Створення компонента GroupDialog**
  Поля: `TextField` (назва), `IntegerField` (курс, чисельність), `TextField` (кафедра).

- [ ] **Крок 2: Зв'язування з бекендом**
  Виклик `GroupEndpoint.saveGroup()` та оновлення списку.

---

### Завдання 4: Фіналізація та Полірування

- [ ] **Крок 1: Стилізація Tailwind**
  Адаптація під стиль «Списку аудиторій».

- [ ] **Крок 2: Підтвердження видалення**
  Використання `ConfirmDialog` та `Notification` (українською).
