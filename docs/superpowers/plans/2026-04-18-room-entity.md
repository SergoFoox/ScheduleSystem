# План впровадження сутності Room

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Впровадження доменної сутності `Room` (Аудиторія) для зберігання інформації про фізичні навчальні приміщення.

**Архітектура:** JPA-сутність у пакеті `com.sergofoox.domain.room`, з повторним використанням `RoomType` із домену `plan`.

**Технологічний стек:** Java 21, Spring Boot 4.x, Jakarta Persistence, Jakarta Validation, JUnit 5.

---

### Завдання 1: Впровадження сутності `Room`

**Файли:**
- Створити: `src/main/java/com/sergofoox/domain/room/Room.java`
- Створити: `src/test/java/com/sergofoox/domain/room/RoomTest.java`

- [ ] **Крок 1: Написання тесту для створення Room та перевірки рівності, що не проходить**

```java
package com.sergofoox.domain.room;

import com.sergofoox.domain.plan.RoomType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoomTest {
    @Test
    void testRoomCreation() {
        Room room = new Room("101", 30, "Main Building", "Projector, PC", RoomType.LECTURE_HALL);
        assertEquals("101", room.getName());
        assertEquals(30, room.getCapacity());
        assertEquals("Main Building", room.getBuilding());
        assertEquals("Projector, PC", room.getEquipment());
        assertEquals(RoomType.LECTURE_HALL, room.getType());
    }

    @Test
    void testEquality() {
        Room r1 = new Room("101", 30, "Main", "Proj", RoomType.LECTURE_HALL);
        Room r2 = new Room("101", 50, "Main", "None", RoomType.LABORATORY);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
```

- [ ] **Крок 2: Запуск тесту для підтвердження невдачі**

Виконати: `./mvnw test -Dtest=RoomTest`
Очікується: ПОМИЛКА (Помилка компіляції: клас Room не існує)

- [ ] **Крок 3: Впровадження сутності Room**

```java
package com.sergofoox.domain.room;

import com.sergofoox.domain.plan.RoomType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Objects;

@Entity
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer capacity;

    @NotBlank
    @Column(nullable = false)
    private String building;

    @Column
    private String equipment;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType type;

    public Room() {}

    public Room(String name, Integer capacity, String building, String equipment, RoomType type) {
        this.name = name;
        this.capacity = capacity;
        this.building = building;
        this.equipment = equipment;
        this.type = type;
    }

    public Room(Long id, String name, Integer capacity, String building, String equipment, RoomType type) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.building = building;
        this.equipment = equipment;
        this.type = type;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }
    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room room)) return false;
        return Objects.equals(name, room.name) &&
               Objects.equals(building, room.building);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, building);
    }

    @Override
    public String toString() {
        return "Room{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", capacity=" + capacity +
                ", building='" + building + '\'' +
                ", type=" + type +
                '}';
    }
}
```

- [ ] **Крок 4: Запуск тесту для підтвердження успіху**

Виконати: `./mvnw test -Dtest=RoomTest`
Очікується: УСПІШНО

- [ ] **Крок 5: Коміт**

```bash
git add src/main/java/com/sergofoox/domain/room/Room.java src/test/java/com/sergofoox/domain/room/RoomTest.java
git commit -m "feat: add Room entity"
```
