# План впровадження: Машина часу (Автозбереження системи)

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Впровадження механізму автоматичного фонового збереження всього стану системи з інтерфейсом для відновлення.

**Архітектура:** Серіалізація всіх доменних сутностей у JSON-знімок, збереження в БД за розкладом (Spring Scheduler) та надання UI для перегляду історії та вибіркового відновлення.

**Технологічний стек:** Java 21, Spring Boot, Spring Data JPA, Jackson, Vaadin Hilla (React + TypeScript), Tailwind CSS.

---

### Завдання 1: Модель даних та Репозиторій

**Файли:**
- Створити: `src/main/java/com/sergofoox/domain/autosave/AutosaveSnapshot.java`
- Створити: `src/main/java/com/sergofoox/domain/autosave/AutosaveRepository.java`
- Створити міграцію: `src/main/resources/db/migration/V5__Autosave_Table.sql`

- [ ] **Крок 1: Створення SQL-міграції**
```sql
CREATE TABLE IF NOT EXISTS autosave_snapshot (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    snapshot_data TEXT NOT NULL,
    entity_count INTEGER NOT NULL
);
```

- [ ] **Крок 2: Створення сутності AutosaveSnapshot**
```java
package com.sergofoox.domain.autosave;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class AutosaveSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime timestamp;
    @Column(columnDefinition = "TEXT")
    private String snapshotData;
    private Integer entityCount;

    public AutosaveSnapshot() {}
    public AutosaveSnapshot(LocalDateTime timestamp, String snapshotData, Integer entityCount) {
        this.timestamp = timestamp;
        this.snapshotData = snapshotData;
        this.entityCount = entityCount;
    }
    // Getters/Setters
}
```

- [ ] **Крок 3: Створення репозиторію**
```java
package com.sergofoox.domain.autosave;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AutosaveRepository extends JpaRepository<AutosaveSnapshot, Long> {
    List<AutosaveSnapshot> findAllByOrderByTimestampDesc();
}
```

---

### Завдання 2: Сервіс автозбереження та Скедулер

**Файли:**
- Створити: `src/main/java/com/sergofoox/domain/autosave/AutosaveService.java`
- Змінити: `src/main/java/com/sergofoox/ScheduleApplication.java`

- [ ] **Крок 1: Увімкнення планувальника в Application**
Додати `@EnableScheduling` до `ScheduleApplication`.

- [ ] **Крок 2: Реалізація логіки знімка у AutosaveService**
Метод `captureSnapshot()` має збирати дані з усіх репозиторіїв та пакувати в JSON через `ObjectMapper`.

- [ ] **Крок 3: Додавання Scheduled завдання**
```java
@Scheduled(fixedRate = 120000) // Кожні 2 хвилини
public void autoSaveTask() {
    captureSnapshot();
    // Ротація: тримаємо лише останні 5
}
```

---

### Завдання 3: Логіка відновлення (Backend)

**Файли:**
- Змінити: `src/main/java/com/sergofoox/domain/autosave/AutosaveService.java`

- [ ] **Крок 1: Реалізація відновлення як шаблону**
Метод, що створює новий `SavedSchedule` із JSON.

- [ ] **Крок 2: Реалізація повного відкату**
Метод, що очищує поточні таблиці та заповнює їх даними з JSON у транзакції.

---

### Завдання 4: Ендпоінт та DTO

**Файли:**
- Створити: `src/main/java/com/sergofoox/domain/autosave/AutosaveEndpoint.java`
- Створити: `src/main/java/com/sergofoox/domain/autosave/AutosaveSnapshotDTO.java`

- [ ] **Крок 1: Створення DTO для фронтенду**
```java
public record AutosaveSnapshotDTO(Long id, String timestamp, Integer entityCount) {}
```

- [ ] **Крок 2: Створення Hilla Endpoint**
Методи: `getLatestSnapshots()`, `restoreSnapshot(id, asNewTemplate)`.

---

### Завдання 5: Інтерфейс "Машина часу" (Frontend)

**Файли:**
- Створити: `src/main/frontend/components/TimeMachineDialog.tsx`
- Змінити: `src/main/frontend/views/dashboard.tsx`

- [ ] **Крок 1: Створення компонента TimeMachineDialog**
Використати `Dialog`, `VerticalLayout` та список карток зі знімками.

- [ ] **Крок 2: Додавання кнопки "Історія" на Dashboard**
```tsx
<Button theme="tertiary" onClick={() => setTimeMachineOpened(true)}>
  <Icon icon="vaadin:clock" slot="prefix" />
  Історія
</Button>
```

- [ ] **Крок 3: Інтеграція логіки відновлення**
Виклик `AutosaveEndpoint.restoreSnapshot` з повідомленням про успіх та оновленням сітки.

---

### Завдання 6: Фінальна перевірка та тести

- [ ] **Крок 1: Тестування циклу автозбереження**
Перевірити лог консолі на наявність записів про створення знімка.
- [ ] **Крок 2: Тестування відновлення**
Спробувати відновити знімок як новий розклад та як прямий відкат.
- [ ] **Крок 3: Коміт усіх змін**
