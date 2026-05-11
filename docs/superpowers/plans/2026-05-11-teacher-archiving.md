# План впровадження: Архівування викладачів

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Впровадження механізму "м'якого видалення" викладачів з інтерфейсом у вигляді вкладок (Активні/Архів) та фільтрацією в розкладі.

**Архітектура:** Додавання поля `archived` до сутності `Teacher`, оновлення репозиторію та ендпоінтів для підтримки фільтрації, та реалізація Tab-інтерфейсу на фронтенді.

**Технологічний стек:** Java 21, Spring Boot, JPA, Vaadin Hilla (React), Tailwind CSS.

---

### Завдання 1: Модель даних та БД

**Файли:**
- Створити міграцію: `src/main/resources/db/migration/V9__Add_Archived_To_Teacher.sql`
- Модифікувати: `src/main/java/com/sergofoox/domain/teacher/Teacher.java`
- Модифікувати: `src/main/resources/application.properties`

- [ ] **Крок 1: Створення SQL-міграції**
```sql
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;
```

- [ ] **Крок 2: Оновлення сутності Teacher**
Додати поле `private boolean archived = false;` з геттером та сеттером. Оновити конструктори за потреби.

- [ ] **Крок 3: Оновлення конфігурації ініціалізації**
Додати `V9` до `spring.sql.init.schema-locations` у `application.properties`.

---

### Завдання 2: Репозиторій та Бекенд-логіка (Teacher)

**Файли:**
- Модифікувати: `src/main/java/com/sergofoox/domain/teacher/TeacherRepository.java`
- Модифікувати: `src/main/java/com/sergofoox/domain/teacher/TeacherEndpoint.java`

- [ ] **Крок 1: Оновлення репозиторію**
Додати методи:
```java
List<Teacher> findByArchivedFalse();
List<Teacher> findByArchivedTrue();
```

- [ ] **Крок 2: Оновлення логіки видалення в Endpoint**
Змінити `deleteTeacher`, щоб замість `delete()` він робив `teacher.setArchived(true); teacherRepository.save(teacher);`.

- [ ] **Крок 3: Додавання методу відновлення**
Створити `public void restoreTeacher(Long id)` у `TeacherEndpoint`.

---

### Завдання 3: Фільтрація викладачів у системі

**Файли:**
- Модифікувати: `src/main/java/com/sergofoox/domain/ui/ScheduleEndpoint.java`
- Модифікувати: `src/main/java/com/sergofoox/domain/plan/CoursePlanEndpoint.java`

- [ ] **Крок 1: Фільтрація в ScheduleEndpoint**
У методах `getScheduleGridData` та `getReplacementCandidates` використовувати лише тих викладачів, у яких `archived == false` (для нових призначень).

- [ ] **Крок 2: Фільтрація в CoursePlanEndpoint**
При виборі викладача для плану повертати лише активних.

---

### Завдання 4: Інтерфейс викладачів (Вкладки)

**Файли:**
- Модифікувати: `src/main/frontend/views/teachers.tsx`

- [ ] **Крок 1: Додавання Tabs**
Використати `Tabs` та `Tab` від Vaadin. Створити стан для активної вкладки.
- [ ] **Крок 2: Логіка відображення**
Фільтрувати список викладачів на основі обраної вкладки.
- [ ] **Крок 3: Оновлення кнопок на картці**
Для активних — "Архівувати" (кошик). Для архівних — "Відновити" (іконка ↺).

---

### Завдання 5: Візуалізація статусу в розкладі

**Файли:**
- Модифікувати: `src/main/frontend/components/GridCell.tsx`

- [ ] **Крок 1: Додавання іконки архіва**
Якщо викладач у занятті має статус `archived`, відобразити маленьку іконку 📦 поруч із прізвищем.

---

### Завдання 6: Тестування та завершення

- [ ] **Крок 1: Тестування архівування**
Перевірити, що викладач переходить у вкладку "Архів" та зникає з випадаючих списків.
- [ ] **Крок 2: Тестування відновлення**
Перевірити повернення викладача до активних.
- [ ] **Крок 3: Перевірка історичних даних**
Переконатися, що в старих розкладах прізвище архівованого викладача відображається з іконкою 📦.
