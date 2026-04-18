# Teacher Entity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the `Teacher` domain entity as a JPA-managed persistence object.

**Architecture:** JPA Entity following standard POJO patterns in the `com.sergofoox.domain.teacher` package.

**Tech Stack:** Java 21, Spring Boot 4.x (Jakarta Persistence), JUnit 5.

---

### Task 1: Create Teacher Entity and Basic Test

**Files:**
- Create: `src/main/java/com/sergofoox/domain/teacher/Teacher.java`
- Create: `src/test/java/com/sergofoox/domain/teacher/TeacherTest.java`

- [ ] **Step 1: Write the failing test for Teacher instantiation**

```java
package com.sergofoox.domain.teacher;

import com.sergofoox.entity.Teacher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TeacherTest {
    @Test
    void testTeacherCreation() {
        Teacher teacher = new Teacher("Ivanov Ivan", "Computer Science", "Full-time");
        assertEquals("Ivanov Ivan", teacher.getFullName());
        assertEquals("Computer Science", teacher.getDepartment());
        assertEquals("Full-time", teacher.getPositionType());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=TeacherTest`
Expected: FAIL (Compilation error: Teacher class does not exist)

- [ ] **Step 3: Write minimal implementation of Teacher class**

```java
package com.sergofoox.domain.teacher;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String positionType;

    public Teacher() {}

    public Teacher(String fullName, String department, String positionType) {
        this.fullName = fullName;
        this.department = department;
        this.positionType = positionType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getPositionType() { return positionType; }
    public void setPositionType(String positionType) { this.positionType = positionType; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=TeacherTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/sergofoox/domain/teacher/Teacher.java src/test/java/com/sergofoox/domain/teacher/TeacherTest.java
git commit -m "feat: add Teacher entity with basic tests"
```

---

### Task 2: Implement Equals, HashCode, and ToString

**Files:**
- Modify: `src/main/java/com/sergofoox/domain/teacher/Teacher.java`
- Modify: `src/test/java/com/sergofoox/domain/teacher/TeacherTest.java`

- [ ] **Step 1: Write failing tests for equality and toString**

```java
    @Test
    void testEquality() {
        Teacher t1 = new Teacher("Ivanov Ivan", "CS", "Full");
        Teacher t2 = new Teacher("Ivanov Ivan", "CS", "Full");
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void testToString() {
        Teacher teacher = new Teacher("Ivanov Ivan", "CS", "Full");
        assertTrue(teacher.toString().contains("Ivanov Ivan"));
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -Dtest=TeacherTest`
Expected: FAIL (Default Object methods don't match business equality)

- [ ] **Step 3: Implement equals, hashCode, and toString**

```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Teacher teacher = (Teacher) o;
        return java.util.Objects.equals(id, teacher.id) &&
               java.util.Objects.equals(fullName, teacher.fullName) &&
               java.util.Objects.equals(department, teacher.department) &&
               java.util.Objects.equals(positionType, teacher.positionType);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, fullName, department, positionType);
    }

    @Override
    public String toString() {
        return "Teacher{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", department='" + department + '\'' +
                ", positionType='" + positionType + '\'' +
                '}';
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -Dtest=TeacherTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/sergofoox/domain/teacher/Teacher.java src/test/java/com/sergofoox/domain/teacher/TeacherTest.java
git commit -m "feat: implement equals, hashCode, and toString for Teacher"
```
