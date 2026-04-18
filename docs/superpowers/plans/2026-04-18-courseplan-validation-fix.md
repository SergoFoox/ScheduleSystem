# CoursePlan Consistency Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a validation to `CoursePlan` to ensure that `totalHours` matches the sum of `lectureHours`, `practiceHours`, and `labHours`.

**Architecture:** Use Bean Validation API (`@AssertTrue`) in the domain entity.

**Tech Stack:** Java 21, Spring Boot 4.0.5, Jakarta Bean Validation.

---

### Task 1: Update CoursePlanTest.java

**Files:**
- Modify: `src/test/java/com/sergofoox/domain/plan/CoursePlanTest.java`

- [ ] **Step 1: Update the validation message check in `testTotalHoursValidation` to match the required message.**

The user requested: "Total hours must equal the sum of lecture, practice, and lab hours"

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
            .anyMatch(v -> v.getMessage().equals("Total hours must equal the sum of lecture, practice, and lab hours"));
        assertTrue(hasConsistencyViolation, "Expected consistency violation message: 'Total hours must equal the sum of lecture, practice, and lab hours'");
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=CoursePlanTest`
Expected: FAIL (still fails because validation is missing)

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/sergofoox/domain/plan/CoursePlanTest.java
git commit -m "test: update CoursePlanTest validation message"
```

---

### Task 2: Implement validation in CoursePlan.java

**Files:**
- Modify: `src/main/java/com/sergofoox/domain/plan/CoursePlan.java`

- [ ] **Step 1: Add @AssertTrue method to CoursePlan class.**

```java
    @jakarta.validation.constraints.AssertTrue(message = "Total hours must equal the sum of lecture, practice, and lab hours")
    public boolean isHoursConsistent() {
        if (totalHours == null || lectureHours == null || practiceHours == null || labHours == null) {
            return true; // Let @NotNull handle nulls
        }
        return totalHours == (lectureHours + practiceHours + labHours);
    }
```

- [ ] **Step 2: Add import for AssertTrue if not already present.**

```java
import jakarta.validation.constraints.AssertTrue;
```

- [ ] **Step 3: Run test to verify it passes**

Run: `./mvnw test -Dtest=CoursePlanTest`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/sergofoox/domain/plan/CoursePlan.java
git commit -m "feat: add hours consistency validation to CoursePlan"
```

---

### Task 3: Final Verification

- [ ] **Step 1: Run all tests in the project.**

Run: `./mvnw test`
Expected: ALL PASS

- [ ] **Step 2: Summary and completion.**
