# ASMS V3 Domain Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix Group, Subject, and CoursePlan entities based on code quality review: stable business keys for Group, pattern matching for Subject equality, and total hours consistency for CoursePlan.

**Architecture:** Surgical updates to existing domain entities and their unit tests.

**Tech Stack:** Java 21, Spring Boot 4.0.5, JUnit 5, Jakarta Validation.

---

### Task 1: Update Group equality logic

**Files:**
- Modify: `src/main/java/com/sergofoox/domain/group/Group.java`
- Modify: `src/test/java/com/sergofoox/domain/group/GroupTest.java`

- [ ] **Step 1: Update GroupTest.java with failing test**
Update `testEquality` to check that groups with different course but same name and department are equal.

```java
    @Test
    void testEquality() {
        Group g1 = new Group("KB-41", 25, 4, "CS");
        Group g2 = new Group("KB-41", 25, 3, "CS"); // Different course, should still be equal
        assertEquals(g1, g2, "Groups with same name and department should be equal regardless of course");
        assertEquals(g1.hashCode(), g2.hashCode());
    }
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./mvnw test -Dtest=GroupTest`
Expected: FAIL

- [ ] **Step 3: Update Group.java equality logic**
Remove `course` from `equals()` and `hashCode()`.

```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group group)) return false;
        return Objects.equals(name, group.name) &&
               Objects.equals(department, group.department);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, department);
    }
```

- [ ] **Step 4: Run test to verify it passes**
Run: `./mvnw test -Dtest=GroupTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add src/main/java/com/sergofoox/domain/group/Group.java src/test/java/com/sergofoox/domain/group/GroupTest.java
git commit -m "refactor(domain): update Group equality to use stable business keys (name, department)"
```

### Task 2: Update Subject equality pattern matching

**Files:**
- Modify: `src/main/java/com/sergofoox/domain/subject/Subject.java`
- Test: `src/test/java/com/sergofoox/domain/subject/SubjectTest.java`

- [ ] **Step 1: Update Subject.java equality pattern matching**
Use pattern matching for `instanceof` in `equals()`.

```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subject subject)) return false;
        return Objects.equals(name, subject.name) &&
               Objects.equals(abbreviation, subject.abbreviation);
    }
```

- [ ] **Step 2: Run test to verify it passes**
Run: `./mvnw test -Dtest=SubjectTest`
Expected: PASS

- [ ] **Step 3: Commit**
```bash
git add src/main/java/com/sergofoox/domain/subject/Subject.java
git commit -m "style(domain): update Subject equality to use pattern matching for instanceof"
```

### Task 3: Add CoursePlan consistency validation

**Files:**
- Modify: `src/main/java/com/sergofoox/domain/plan/CoursePlan.java`
- Modify: `src/test/java/com/sergofoox/domain/plan/CoursePlanTest.java`

- [ ] **Step 1: Add validation test to CoursePlanTest.java**
Add a test case that checks for validation violations when total hours are inconsistent.

```java
    @Test
    void testTotalHoursValidation() {
        jakarta.validation.ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
        jakarta.validation.Validator validator = factory.getValidator();

        Subject s = new Subject("Math", "M");
        Group g = new Group("KB-41", 25, 4, "CS");
        
        // Correct total hours
        CoursePlan validPlan = new CoursePlan(s, g, 120, 40, 40, 40, 1, 1, 1, RoomType.LECTURE_HALL);
        assertTrue(validator.validate(validPlan).isEmpty(), "Valid plan should have no violations");

        // Incorrect total hours (120 != 40 + 40 + 30)
        CoursePlan invalidPlan = new CoursePlan(s, g, 120, 40, 40, 30, 1, 1, 1, RoomType.LECTURE_HALL);
        var violations = validator.validate(invalidPlan);
        assertFalse(violations.isEmpty(), "Incorrect total hours should produce validation error");
        boolean hasConsistencyViolation = violations.stream()
            .anyMatch(v -> v.getMessageTemplate().contains("Total hours must match the sum of lecture, practice and lab hours"));
        assertTrue(hasConsistencyViolation, "Expected consistency violation message");
    }
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./mvnw test -Dtest=CoursePlanTest`
Expected: FAIL (on `hasConsistencyViolation`)

- [ ] **Step 3: Add validation logic to CoursePlan.java**
Add `@AssertTrue` method.

```java
    @jakarta.validation.constraints.AssertTrue(message = "Total hours must match the sum of lecture, practice and lab hours")
    public boolean isTotalHoursConsistent() {
        if (totalHours == null || lectureHours == null || practiceHours == null || labHours == null) {
            return true; // Let @NotNull handle null checks
        }
        return totalHours.equals(lectureHours + practiceHours + labHours);
    }
```

- [ ] **Step 4: Run test to verify it passes**
Run: `./mvnw test -Dtest=CoursePlanTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add src/main/java/com/sergofoox/domain/plan/CoursePlan.java src/test/java/com/sergofoox/domain/plan/CoursePlanTest.java
git commit -m "feat(domain): add consistency validation for CoursePlan total hours"
```
