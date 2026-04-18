# Cleanup & Standardization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor domain entities for better validation, JPA safety, and standardization.

**Architecture:** Update entities with Jakarta Validation constraints and Java 16+ pattern matching in `equals()`. Standardize business keys for `equals` and `hashCode`.

**Tech Stack:** Java 21, Spring Boot, Jakarta Validation, JPA.

---

### Task 1: Teacher Entity Refactor

**Files:**
- Modify: `src/main/java/com/sergofoox/domain/teacher/Teacher.java`
- Test: `src/test/java/com/sergofoox/domain/teacher/TeacherTest.java`

- [ ] **Step 1: Update Teacher.java with constraints and pattern matching**

```java
package com.sergofoox.domain.teacher;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

@Entity
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Column(nullable = false)
    private String fullName;

    @NotBlank(message = "Department is required")
    @Column(nullable = false)
    private String department;

    @NotBlank(message = "Position type is required")
    @Column(nullable = false)
    private String positionType;

    public Teacher() {}

    public Teacher(String fullName, String department, String positionType) {
        this.fullName = fullName;
        this.department = department;
        this.positionType = positionType;
    }

    public Teacher(Long id, String fullName, String department, String positionType) {
        this.id = id;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Teacher other)) return false;
        return Objects.equals(fullName, other.fullName) &&
               Objects.equals(department, other.department) &&
               Objects.equals(positionType, other.positionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName, department, positionType);
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
}
```

- [ ] **Step 2: Run tests to verify Teacher changes**

Run: `./mvnw test -Dtest=TeacherTest`
Expected: PASS

### Task 2: Standardize Room Entity

**Files:**
- Modify: `src/main/java/com/sergofoox/domain/room/Room.java`
- Test: `src/test/java/com/sergofoox/domain/room/RoomTest.java`

- [ ] **Step 1: Update Room.java with pattern matching in equals**

```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room other)) return false;
        return Objects.equals(name, other.name) && Objects.equals(building, other.building);
    }
```

- [ ] **Step 2: Run tests to verify Room changes**

Run: `./mvnw test -Dtest=RoomTest`
Expected: PASS

### Task 3: Standardize TeacherCompetenceMatrix Entity

**Files:**
- Modify: `src/main/java/com/sergofoox/domain/competence/TeacherCompetenceMatrix.java`
- Test: `src/test/java/com/sergofoox/domain/competence/TeacherCompetenceMatrixTest.java`

- [ ] **Step 1: Update TeacherCompetenceMatrix.java with pattern matching in equals**

```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeacherCompetenceMatrix other)) return false;
        return Objects.equals(teacher, other.teacher) &&
               Objects.equals(subject, other.subject) &&
               lessonType == other.lessonType;
    }
```

- [ ] **Step 2: Run tests to verify Matrix changes**

Run: `./mvnw test -Dtest=TeacherCompetenceMatrixTest`
Expected: PASS

### Task 4: Standardize Other Entities (Verification)

**Files:**
- Modify: `src/main/java/com/sergofoox/domain/subject/Subject.java`
- Modify: `src/main/java/com/sergofoox/domain/group/Group.java`
- Modify: `src/main/java/com/sergofoox/domain/plan/CoursePlan.java`

- [ ] **Step 1: Verify and ensure pattern matching in Subject.java** (Already seems correct, but double check)
- [ ] **Step 2: Verify and ensure pattern matching in Group.java** (Already seems correct, but double check)
- [ ] **Step 3: Verify and ensure pattern matching in CoursePlan.java** (Already seems correct, but double check)

### Task 5: Memory Bank Update

**Files:**
- Modify: `memory-bank/progress.md`
- Modify: `memory-bank/activeContext.md`

- [ ] **Step 1: Update progress.md**
  - Mark `Subject`, `Group`, `CoursePlan`, `Room`, and `Matrix` as completed.
  - Move "Define Domain Model" to Completed.
  - Add "Timefold Integration" to In Progress.
- [ ] **Step 2: Update activeContext.md**
  - Update focus to "Timefold Integration and Solver configuration".
  - Update next steps to "Set up Timefold Solver and define initial constraints".

### Task 6: Final Verification & Commit

- [ ] **Step 1: Run all domain tests**

Run: `./mvnw test`
Expected: PASS

- [ ] **Step 2: Commit changes**

```bash
git add .
git commit -m "refactor: cleanup and standardize domain entities

- Add @NotBlank to Teacher fields
- Use Java 16+ pattern matching for equals() in all entities
- Ensure stable business keys for equals/hashCode
- Update Memory Bank to reflect completed domain model"
```
