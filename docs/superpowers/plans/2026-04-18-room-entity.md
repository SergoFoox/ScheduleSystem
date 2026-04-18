# Room Entity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the `Room` (Аудиторія) domain entity to store physical classroom information.

**Architecture:** JPA Entity in the `com.sergofoox.domain.room` package, reusing `RoomType` from the `plan` domain.

**Tech Stack:** Java 21, Spring Boot 4.x, Jakarta Persistence, Jakarta Validation, JUnit 5.

---

### Task 1: Implement `Room` Entity

**Files:**
- Create: `src/main/java/com/sergofoox/domain/room/Room.java`
- Create: `src/test/java/com/sergofoox/domain/room/RoomTest.java`

- [ ] **Step 1: Write the failing test for Room instantiation and equality**

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

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=RoomTest`
Expected: FAIL (Compilation error: Room class does not exist)

- [ ] **Step 3: Implement Room Entity**

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

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=RoomTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/sergofoox/domain/room/Room.java src/test/java/com/sergofoox/domain/room/RoomTest.java
git commit -m "feat: add Room entity"
```
