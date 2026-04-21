# План впровадження інтерфейсу та дашборда (ASMS V3)

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Реалізація реактивного дашборда на основі Hilla (React) для візуалізації розкладу, аналітики навантаження та модуля швидких замін.

**Архітектура:** Hilla (React) фронтенд з `@BrowserCallable` Java-ендпоінтами. Використання Signals для реактивності.

**Технологічний стек:** Vaadin 25.1.2, React 19, Preact Signals, Spring Security.

---

### Завдання 1: Налаштування структури Hilla та головного макета

**Files:**
- Створити: `src/main/frontend/views/@layout.tsx`
- Створити: `src/main/frontend/views/@index.tsx`
- Створити: `src/main/frontend/views/dashboard-view.tsx`
- Модифікувати: `pom.xml` (додати hilla-spring-boot-starter якщо відсутній)

- [ ] **Крок 1: Перевірка та додавання залежностей Hilla в pom.xml**
- [ ] **Крок 2: Створення головного макета з Aura Theme та навігацією**
- [ ] **Крок 3: Створення базової сторінки дашборда**

---

### Завдання 2: Створення DTO та Java-ендпоінтів для фронтенду

**Files:**
- Створити: `src/main/java/com/sergofoox/domain/ui/ScheduleEndpoint.java`
- Створити: `src/main/java/com/sergofoox/domain/ui/dto/LessonDTO.java`
- Створити: `src/main/java/com/sergofoox/domain/ui/dto/ScheduleGridDTO.java`

- [ ] **Крок 1: Визначення DTO для передачі даних розкладу на фронтенд**
- [ ] **Крок 2: Реалізація `@BrowserCallable` сервісу для отримання даних сітки**
- [ ] **Крок 3: Генерація TypeScript клієнтів (`./mvnw compile`)**

---

### Завдання 3: Реалізація адаптивної сітки розкладу (Responsive Grid)

**Files:**
- Створити: `src/main/frontend/components/ScheduleGrid.tsx`
- Створити: `src/main/frontend/components/GridCell.tsx`

- [ ] **Крок 1: Створення компонента сітки з підтримкою перемикання осей (Група/Викладач/Аудиторія)**
- [ ] **Крок 2: Реалізація відображення занять у комірках таймслотів**
- [ ] **Крок 3: Додавання базової стилізації Aura для сітки**

---

### Завдання 4: Реактивна бічна панель (Analytics Sidebar) з Signals

**Files:**
- Створити: `src/main/frontend/store/app-state.ts`
- Створити: `src/main/frontend/components/AnalyticsSidebar.tsx`

- [ ] **Крок 1: Налаштування Signals для глобального стану (обрана група/викладач)**
- [ ] **Крок 2: Створення віджетів аналітики невичитаних годин**
- [ ] **Крок 3: Зв'язування кліку по сітці з оновленням сигналу**

---

### Завдання 5: Модуль «Швидкі заміни» та інтерактивний підбір

**Files:**
- Створити: `src/main/frontend/components/ReplacementDialog.tsx`
- Модифікувати: `src/main/java/com/sergofoox/domain/ui/ScheduleEndpoint.java` (додати метод пошуку замін)

- [ ] **Крок 1: Реалізація логіки пошуку вільних викладачів на бекенді**
- [ ] **Крок 2: Створення модального вікна підбору замін на фронтенді**
- [ ] **Крок 3: Реалізація операції "Призначити заміну" з миттєвим оновленням UI**

---

### Завдання 6: Управління статусами та безпека

**Files:**
- Створити: `src/main/java/com/sergofoox/config/SecurityConfig.java`
- Модифікувати: `src/main/frontend/views/@layout.tsx` (додати перемикач Draft/Published)

- [ ] **Крок 1: Налаштування Spring Security для ролей DISPATCHER та USER**
- [ ] **Крок 2: Реалізація перемикача режимів «Чернетка/Опубліковано» у хедері**
- [ ] **Крок 3: Обмеження функцій редагування на основі статусу розкладу**
