# Subject and TeacherCompetenceMatrix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the `Subject` entity and the `TeacherCompetenceMatrix` entity to define teacher competencies.

**Architecture:** JPA Entities following standard POJO patterns in `com.sergofoox.entity` and `com.sergofoox.domain.competence`.

**Tech Stack:** Java 21, Spring Boot 4.x (Jakarta Persistence), JUnit 5.

---

### Task 1: Implement `Subject` Entity and `LessonType` Enum

**Files:**
- Create: `src/main/java/com/sergofoox/entity/Subject.java`
- Create: `src/main/java/com/sergofoox/entity/LessonType.java`
- Create: `src/test/java/com/sergofoox/entity/SubjectTest.java`

- [ ] **Step 1: Write the failing test for Subject instantiation**

```java
package com.sergofoox.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SubjectTest {
    @Test
    void testSubjectCreation() {
        Subject subject = new Subject("Mathematics", "Math");
        assertEquals("Mathematics", subject.getName());
        assertEquals("Math", subject.getAbbreviation());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=SubjectTest`
Expected: FAIL (Compilation error: Subject class does not exist)

- [ ] **Step 3: Implement LessonType Enum**

```java
package com.sergofoox.entity;

public enum LessonType {
    LECTURE,
    PRACTICE,
    LABORATORY
}
```

- [ ] **Step 4: Implement Subject Entity**

```java
package com.sergofoox.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String abbreviation;

    public Subject() {}

    public Subject(String name, String abbreviation) {
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public Subject(Long id, String name, String abbreviation) {
        this.id = id;
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subject subject = (Subject) o;
        return Objects.equals(id, subject.id) &&
               Objects.equals(name, subject.name) &&
               Objects.equals(abbreviation, subject.abbreviation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, abbreviation);
    }

    @Override
    public String toString() {
        return "Subject{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", abbreviation='" + abbreviation + '\'' +
                '}';
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw test -Dtest=SubjectTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/sergofoox/entity/Subject.java src/main/java/com/sergofoox/entity/LessonType.java src/test/java/com/sergofoox/entity/SubjectTest.java
git commit -m "feat: add Subject entity and LessonType enum"
```

---

### Task 2: Implement `Priority` Enum and `TeacherCompetenceMatrix` Entity

**Files:**
- Create: `src/main/java/com/sergofoox/domain/competence/Priority.java`
- Create: `src/main/java/com/sergofoox/domain/competence/TeacherCompetenceMatrix.java`
- Create: `src/test/java/com/sergofoox/domain/competence/TeacherCompetenceMatrixTest.java`

- [ ] **Step 1: Write failing test for TeacherCompetenceMatrix creation**

```java
package com.sergofoox.domain.competence;

import com.sergofoox.entity.Teacher;
import com.sergofoox.entity.Subject;
import com.sergofoox.entity.LessonType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeacherCompetenceMatrixTest {
    @Test
    void testMatrixCreation() {
        Teacher teacher = new Teacher("Ivanov Ivan", "CS", "Full");
        Subject subject = new Subject("Mathematics", "Math");
        TeacherCompetenceMatrix matrix = new TeacherCompetenceMatrix(teacher, subject, LessonType.LECTURE, Priority.HIGH);
        
        assertEquals(teacher, matrix.getTeacher());
        assertEquals(subject, matrix.getSubject());
        assertEquals(LessonType.LECTURE, matrix.getLessonType());
        assertEquals(Priority.HIGH, matrix.getPriority());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=TeacherCompetenceMatrixTest`
Expected: FAIL (Compilation error: Priority and TeacherCompetenceMatrix classes do not exist)

- [ ] **Step 3: Implement Priority Enum**

```java
package com.sergofoox.domain.competence;

public enum Priority {
    HIGH,
    MEDIUM,
    LOW
}
```

- [ ] **Step 4: Implement TeacherCompetenceMatrix Entity**

```java
package com.sergofoox.domain.competence;

import com.sergofoox.entity.Teacher;
import com.sergofoox.entity.Subject;
import com.sergofoox.entity.LessonType;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
public class TeacherCompetenceMatrix {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonType lessonType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    public TeacherCompetenceMatrix() {}

    public TeacherCompetenceMatrix(Teacher teacher, Subject subject, LessonType lessonType, Priority priority) {
        this.teacher = teacher;
        this.subject = subject;
        this.lessonType = lessonType;
        this.priority = priority;
    }

    public TeacherCompetenceMatrix(Long id, Teacher teacher, Subject subject, LessonType lessonType, Priority priority) {
        this.id = id;
        this.teacher = teacher;
        this.subject = subject;
        this.lessonType = lessonType;
        this.priority = priority;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public LessonType getLessonType() { return lessonType; }
    public void setLessonType(LessonType lessonType) { this.lessonType = lessonType; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeacherCompetenceMatrix that = (TeacherCompetenceMatrix) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(teacher, that.teacher) &&
               Objects.equals(subject, that.subject) &&
               lessonType == that.lessonType &&
               priority == that.priority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, teacher, subject, lessonType, priority);
    }

    @Override
    public String toString() {
        return "TeacherCompetenceMatrix{" +
                "id=" + id +
                ", teacher=" + teacher +
                ", subject=" + subject +
                ", lessonType=" + lessonType +
                ", priority=" + priority +
                '}';
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw test -Dtest=TeacherCompetenceMatrixTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/sergofoox/domain/competence/Priority.java src/main/java/com/sergofoox/domain/competence/TeacherCompetenceMatrix.java src/test/java/com/sergofoox/domain/competence/TeacherCompetenceMatrixTest.java
git commit -m "feat: add TeacherCompetenceMatrix entity and Priority enum"
```
