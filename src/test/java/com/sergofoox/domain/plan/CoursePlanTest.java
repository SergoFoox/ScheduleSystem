package com.sergofoox.domain.plan;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.PositionType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoursePlanTest {
    @Test
    void testPlanCreation() {
        Subject subject = new Subject("Math", "M");
        Teacher teacher = new Teacher("Smith", "CS", PositionType.FULL_TIME);
        Group group = new Group("KB-41", 25, 4, "CS");
        CoursePlan plan = new CoursePlan(subject, teacher, group, 120, 40, 40, 40, 1, 1, 1, RoomType.LECTURE_HALL);
        
        assertEquals(subject, plan.getSubject());
        assertEquals(teacher, plan.getTeacher());
        assertEquals(group, plan.getGroup());
        assertEquals(120, plan.getTotalHours());
        assertEquals(RoomType.LECTURE_HALL, plan.getRequiredRoomType());
    }

    @Test
    void testEquality() {
        Subject s1 = new Subject("Math", "M");
        Teacher t1 = new Teacher("Smith", "CS", PositionType.FULL_TIME);
        Group g1 = new Group("KB-41", 25, 4, "CS");
        CoursePlan p1 = new CoursePlan(s1, t1, g1, 120, 40, 40, 40, 1, 1, 1, RoomType.LECTURE_HALL);
        CoursePlan p2 = new CoursePlan(s1, t1, g1, 120, 40, 40, 40, 1, 1, 1, RoomType.LECTURE_HALL);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void testTotalHoursValidation() {
        jakarta.validation.ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
        jakarta.validation.Validator validator = factory.getValidator();

        Subject s = new Subject("Math", "M");
        Teacher t = new Teacher("Smith", "CS", PositionType.FULL_TIME);
        Group g = new Group("KB-41", 25, 4, "CS");
        
        // Correct total hours
        CoursePlan validPlan = new CoursePlan(s, t, g, 120, 40, 40, 40, 1, 1, 1, RoomType.LECTURE_HALL);
        assertTrue(validator.validate(validPlan).isEmpty(), "Valid plan should have no violations");

        // Incorrect total hours (120 != 40 + 40 + 30)
        CoursePlan invalidPlan = new CoursePlan(s, t, g, 120, 40, 40, 30, 1, 1, 1, RoomType.LECTURE_HALL);
        var violations = validator.validate(invalidPlan);
        assertFalse(violations.isEmpty(), "Incorrect total hours should produce validation error");
        boolean hasConsistencyViolation = violations.stream()
            .anyMatch(v -> v.getMessage().equals("Total hours must equal the sum of lecture, practice, and lab hours"));
        assertTrue(hasConsistencyViolation, "Expected consistency violation message: 'Total hours must equal the sum of lecture, practice, and lab hours'");
    }
}
