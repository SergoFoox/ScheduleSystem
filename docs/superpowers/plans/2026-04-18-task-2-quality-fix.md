# Teacher Relocation and Entity Refinement Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Relocate the `Teacher` entity to the domain package and refine entity logic (equality, lazy fetching, validation) for `Teacher`, `Subject`, and `TeacherCompetenceMatrix`.

**Architecture:** Domain-Driven Design (DDD) alignment by moving entities to domain packages. Standardizing JPA entity equality using business keys and `instanceof`. Improving performance with lazy loading.

**Tech Stack:** Java 21, Spring Boot 4.0.5, Jakarta Persistence (JPA), JUnit 5, Jakarta Validation.

---

### Task 1: Relocate Teacher.java

**Files:**
- Move: `src/main/java/com/sergofoox/entity/Teacher.java` to `src/main/java/com/sergofoox/domain/teacher/Teacher.java`
- Modify: `src/main/java/com/sergofoox/domain/teacher/Teacher.java` (package update)

- [ ] **Step 1: Move the file**
Run: `mkdir -p src/main/java/com/sergofoox/domain/teacher && mv src/main/java/com/sergofoox/entity/Teacher.java src/main/java/com/sergofoox/domain/teacher/Teacher.java`

- [ ] **Step 2: Update package declaration in Teacher.java**
```java
package com.sergofoox.domain.teacher;
```

- [ ] **Step 3: Standardize equals() in Teacher.java**
Use `instanceof` and only business keys (`fullName`, `department`, `positionType`).
```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Teacher)) return false;
        Teacher teacher = (Teacher) o;
        return java.util.Objects.equals(fullName, teacher.fullName) &&
               java.util.Objects.equals(department, teacher.department) &&
               java.util.Objects.equals(positionType, teacher.positionType);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(fullName, department, positionType);
    }
```

- [ ] **Step 4: Commit**
```bash
git add src/main/java/com/sergofoox/domain/teacher/Teacher.java
git rm src/main/java/com/sergofoox/entity/Teacher.java
git commit -m "refactor: relocate Teacher entity to domain package and refine equality"
```

### Task 2: Update TeacherCompetenceMatrix.java

**Files:**
- Modify: `src/main/java/com/sergofoox/domain/competence/TeacherCompetenceMatrix.java`

- [ ] **Step 1: Update imports and add @NotNull**
Update `Teacher` import. Add `jakarta.validation.constraints.NotNull`.

- [ ] **Step 2: Apply FetchType.LAZY and @NotNull to fields**
```java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @NotNull
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @NotNull
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private LessonType lessonType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private Priority priority;
```

- [ ] **Step 3: Refine equals() and hashCode()**
Use business keys: `teacher`, `subject`, `lessonType`. Use `instanceof`.
```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeacherCompetenceMatrix)) return false;
        TeacherCompetenceMatrix that = (TeacherCompetenceMatrix) o;
        return Objects.equals(teacher, that.teacher) &&
               Objects.equals(subject, that.subject) &&
               lessonType == that.lessonType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(teacher, subject, lessonType);
    }
```

- [ ] **Step 4: Commit**
```bash
git add src/main/java/com/sergofoox/domain/competence/TeacherCompetenceMatrix.java
git commit -m "refactor: update TeacherCompetenceMatrix with lazy loading, validation, and business key equality"
```

### Task 3: Standardize Subject.java

**Files:**
- Modify: `src/main/java/com/sergofoox/domain/subject/Subject.java`

- [ ] **Step 1: Verify equals() uses instanceof** (Already done in research, but ensuring consistency in plan)

- [ ] **Step 2: Commit (if any changes made)**
```bash
git add src/main/java/com/sergofoox/domain/subject/Subject.java
git commit -m "style: ensure Subject equality standardization"
```

### Task 4: Update All Tests

**Files:**
- Modify: `src/test/java/com/sergofoox/domain/teacher/TeacherTest.java`
- Modify: `src/test/java/com/sergofoox/domain/subject/SubjectTest.java`
- Modify: `src/test/java/com/sergofoox/domain/competence/TeacherCompetenceMatrixTest.java`

- [ ] **Step 1: Update TeacherTest.java**
Fix package and imports. Add test for equality without ID.
```java
package com.sergofoox.domain.teacher;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeacherTest {
    @Test
    void testEqualityWithoutId() {
        Teacher t1 = new Teacher(1L, "Ivanov Ivan", "CS", "Full");
        Teacher t2 = new Teacher(2L, "Ivanov Ivan", "CS", "Full");
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }
}
```

- [ ] **Step 2: Update TeacherCompetenceMatrixTest.java**
Fix imports. Add test for business key equality.
```java
package com.sergofoox.domain.competence;

import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.LessonType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeacherCompetenceMatrixTest {
    @Test
    void testBusinessKeyEquality() {
        Teacher t = new Teacher("Ivan", "CS", "Full");
        Subject s = new Subject("Math", "M");
        TeacherCompetenceMatrix m1 = new TeacherCompetenceMatrix(1L, t, s, LessonType.LECTURE, Priority.HIGH);
        TeacherCompetenceMatrix m2 = new TeacherCompetenceMatrix(2L, t, s, LessonType.LECTURE, Priority.LOW);
        
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }
}
```

- [ ] **Step 3: Run all tests**
Run: `./mvnw test`

- [ ] **Step 4: Commit tests**
```bash
git add src/test/java/com/sergofoox/domain/teacher/TeacherTest.java src/test/java/com/sergofoox/domain/competence/TeacherCompetenceMatrixTest.java src/test/java/com/sergofoox/domain/subject/SubjectTest.java
git commit -m "test: update tests for relocated entities and new equality logic"
```
