# Task 1 Quality Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix Task 1 implementation by relocating files to the correct domain package, adding bean validation, and improving test coverage and JPA-safe equals/hashCode.

**Architecture:** Domain-Driven Design (DDD) approach by moving entities to domain packages. Jakarta Bean Validation for data integrity. JPA-safe equals/hashCode based on business keys or consistent identifier handling.

**Tech Stack:** Java 21, Spring Boot 4.0.5, Jakarta Persistence, Jakarta Validation, JUnit 5.

---

### Task 1: Relocation and Package Refactoring

**Files:**
- Move: `src/main/java/com/sergofoox/entity/Subject.java` -> `src/main/java/com/sergofoox/domain/subject/Subject.java`
- Move: `src/main/java/com/sergofoox/entity/LessonType.java` -> `src/main/java/com/sergofoox/domain/subject/LessonType.java`
- Move: `src/test/java/com/sergofoox/entity/SubjectTest.java` -> `src/test/java/com/sergofoox/domain/subject/SubjectTest.java`

- [ ] **Step 1: Create target directories**
Run: `mkdir -p src/main/java/com/sergofoox/domain/subject src/test/java/com/sergofoox/domain/subject`

- [ ] **Step 2: Relocate files**
Run: `mv src/main/java/com/sergofoox/entity/Subject.java src/main/java/com/sergofoox/domain/subject/`
Run: `mv src/main/java/com/sergofoox/entity/LessonType.java src/main/java/com/sergofoox/domain/subject/`
Run: `mv src/test/java/com/sergofoox/entity/SubjectTest.java src/test/java/com/sergofoox/domain/subject/`

- [ ] **Step 3: Update package declarations and imports**
Update `Subject.java`, `LessonType.java`, and `SubjectTest.java` to use `package com.sergofoox.domain.subject;`.

- [ ] **Step 4: Verify project compiles**
Run: `./mvnw compile`
Expected: SUCCESS

- [ ] **Step 5: Commit relocation**
```bash
git add src/main/java/com/sergofoox/domain/subject src/test/java/com/sergofoox/domain/subject src/main/java/com/sergofoox/entity/ src/test/java/com/sergofoox/entity/
git commit -m "refactor: relocate Subject and LessonType to domain package"
```

### Task 2: Expand SubjectTest with TDD (Red Phase)

**Files:**
- Modify: `src/test/java/com/sergofoox/domain/subject/SubjectTest.java`

- [ ] **Step 1: Add tests for all-args constructor, equals, and hashCode**
Update `SubjectTest.java` with more comprehensive tests.

```java
package com.sergofoox.domain.subject;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SubjectTest {
    @Test
    void testSubjectCreation() {
        Subject subject = new Subject("Mathematics", "Math");
        assertEquals("Mathematics", subject.getName());
        assertEquals("Math", subject.getAbbreviation());
    }

    @Test
    void testAllArgsConstructorAndGetters() {
        Subject subject = new Subject(1L, "Physics", "Phys");
        assertEquals(1L, subject.getId());
        assertEquals("Physics", subject.getName());
        assertEquals("Phys", subject.getAbbreviation());
    }

    @Test
    void testEqualsAndHashCode() {
        Subject s1 = new Subject(1L, "Math", "M");
        Subject s2 = new Subject(1L, "Math", "M");
        Subject s3 = new Subject(2L, "Physics", "P");
        Subject s4 = new Subject(null, "Math", "M");
        Subject s5 = new Subject(null, "Math", "M");

        // Reflexive
        assertEquals(s1, s1);
        
        // Symmetric
        assertEquals(s1, s2);
        assertEquals(s2, s1);
        
        // Consistent with hashCode
        assertEquals(s1.hashCode(), s2.hashCode());
        
        // Not equal
        assertNotEquals(s1, s3);
        assertNotEquals(s1, null);
        assertNotEquals(s1, new Object());

        // Null IDs equality (based on business key if possible, but let's check current impl first)
        // For JPA safety, we might want to compare business keys when ID is null.
        // For now, let's ensure consistency.
        assertEquals(s4, s5);
        assertEquals(s4.hashCode(), s5.hashCode());
    }
}
```

- [ ] **Step 2: Add validation tests (will fail initially)**
Requires `jakarta.validation-api` and an implementation (like Hibernate Validator) in `pom.xml`. Assuming it's there as it's a Spring Boot project.

```java
    @Test
    void testValidation() {
        // This is a unit test, we can use a Validator to check annotations
        jakarta.validation.ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
        jakarta.validation.Validator validator = factory.getValidator();

        Subject invalidSubject = new Subject("", "");
        var violations = validator.validate(invalidSubject);
        assertFalse(violations.isEmpty(), "Should have violations for empty strings");
        
        Subject validSubject = new Subject("Mathematics", "MATH");
        assertTrue(validator.validate(validSubject).isEmpty());
    }
```

- [ ] **Step 3: Run tests and verify failure**
Run: `./mvnw test -Dtest=SubjectTest`
Expected: FAIL (validation test will fail because annotations are missing)

### Task 3: Implement Validation and Refine Equals/HashCode (Green Phase)

**Files:**
- Modify: `src/main/java/com/sergofoox/domain/subject/Subject.java`

- [ ] **Step 1: Add validation annotations**
Add `@NotBlank` and `@Size` to `name` and `abbreviation`.

- [ ] **Step 2: Refine equals and hashCode for JPA safety**
Use only `id` for `equals`/`hashCode` if `id` is present, or a stable business key. For this task, ensure it matches the test expectations.

- [ ] **Step 3: Run tests and verify pass**
Run: `./mvnw test -Dtest=SubjectTest`
Expected: SUCCESS

- [ ] **Step 4: Commit fixes**
```bash
git add src/main/java/com/sergofoox/domain/subject/Subject.java src/test/java/com/sergofoox/domain/subject/SubjectTest.java
git commit -m "feat: add validation and improve Subject entity quality"
```

### Task 4: Final Verification

- [ ] **Step 1: Run all tests in project**
Run: `./mvnw test`
Expected: SUCCESS

- [ ] **Step 2: Check for any remaining references to old package**
Run: `grep -r "com.sergofoox.entity" .`
Expected: No matches (except maybe in target/ or .git/)
