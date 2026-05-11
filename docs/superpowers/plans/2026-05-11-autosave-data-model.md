# План впровадження моделі даних та репозиторію Автозбереження

> **Для агентів:** НЕОБХІДНИЙ ПІД-СКІЛ: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного впровадження цього плану. Кроки використовують синтаксис чекбоксів (`- [ ]`) для відстеження.

**Мета:** Створити структуру бази даних, Java-сутність та репозиторій для зберігання знімків системи (машина часу).

**Архітектура:** Використання Flyway для міграції БД, JPA для сутності та Spring Data JPA для репозиторію. Дані знімка зберігатимуться у текстовому полі великого об'єму.

**Технологічний стек:** Java 21, Spring Boot, Hibernate/JPA, Flyway, PostgreSQL.

---

### Завдання 1: Міграція Flyway

**Файли:**
- Створити: `src/main/resources/db/migration/V5__Autosave_Table.sql`

- [ ] **Крок 1: Створити SQL файл міграції**

```sql
CREATE TABLE IF NOT EXISTS autosave_snapshot (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    snapshot_data TEXT NOT NULL,
    entity_count INTEGER NOT NULL
);
```

### Завдання 2: Java-сутність AutosaveSnapshot

**Файли:**
- Створити: `src/main/java/com/sergofoox/domain/autosave/AutosaveSnapshot.java`

- [ ] **Крок 1: Створити клас сутності**

```java
package com.sergofoox.domain.autosave;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class AutosaveSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String snapshotData;

    @Column(nullable = false)
    private Integer entityCount;

    public AutosaveSnapshot() {}

    public AutosaveSnapshot(LocalDateTime timestamp, String snapshotData, Integer entityCount) {
        this.timestamp = timestamp;
        this.snapshotData = snapshotData;
        this.entityCount = entityCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getSnapshotData() { return snapshotData; }
    public void setSnapshotData(String snapshotData) { this.snapshotData = snapshotData; }
    public Integer getEntityCount() { return entityCount; }
    public void setEntityCount(Integer entityCount) { this.entityCount = entityCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AutosaveSnapshot that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AutosaveSnapshot{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", entityCount=" + entityCount +
                '}';
    }
}
```

### Завдання 3: Репозиторій AutosaveRepository

**Файли:**
- Створити: `src/main/java/com/sergofoox/domain/autosave/AutosaveRepository.java`

- [ ] **Крок 1: Створити інтерфейс репозиторію**

```java
package com.sergofoox.domain.autosave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AutosaveRepository extends JpaRepository<AutosaveSnapshot, Long> {
    List<AutosaveSnapshot> findAllByOrderByTimestampDesc();
}
```

### Завдання 4: Перевірка та Коміт

- [ ] **Крок 1: Компіляція проекту**

Виконати: `./mvnw compile`
Очікуваний результат: BUILD SUCCESS

- [ ] **Крок 2: Коміт змін**

```bash
git add src/main/resources/db/migration/V5__Autosave_Table.sql \
        src/main/java/com/sergofoox/domain/autosave/AutosaveSnapshot.java \
        src/main/java/com/sergofoox/domain/autosave/AutosaveRepository.java
git commit -m "feat: add autosave snapshot data model and repository"
```
