package com.sergofoox.domain.teacher;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeacherTest {
    @Test
    void testTeacherCreation() {
        Teacher teacher = new Teacher("Ivanov Ivan", "Computer Science", PositionType.FULL_TIME);
        assertEquals("Ivanov Ivan", teacher.getFullName());
        assertEquals("Computer Science", teacher.getDepartment());
        assertEquals(PositionType.FULL_TIME, teacher.getPositionType());
        assertEquals(40, teacher.getWeeklyHourLimit()); // Default value
    }

    @Test
    void testTeacherAllArgsConstructor() {
        Teacher teacher = new Teacher(1L, "Petrov Petr", "Mathematics", PositionType.PART_TIME, 20);
        teacher.setMaxWorkingDaysPerWeek(3);
        assertEquals(1L, teacher.getId());
        assertEquals("Petrov Petr", teacher.getFullName());
        assertEquals("Mathematics", teacher.getDepartment());
        assertEquals(PositionType.PART_TIME, teacher.getPositionType());
        assertEquals(20, teacher.getWeeklyHourLimit());
        assertEquals(3, teacher.getMaxWorkingDaysPerWeek());
    }

    @Test
    void testEquality() {
        Teacher t1 = new Teacher(1L, "Ivanov Ivan", "CS", PositionType.FULL_TIME, 40);
        Teacher t2 = new Teacher(1L, "Ivanov Ivan", "CS", PositionType.FULL_TIME, 40);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void testEqualityWithoutId() {
        Teacher t1 = new Teacher(1L, "Ivanov Ivan", "CS", PositionType.FULL_TIME, 40);
        Teacher t2 = new Teacher(2L, "Ivanov Ivan", "CS", PositionType.FULL_TIME, 40);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void testToString() {
        Teacher teacher = new Teacher(1L, "Ivanov Ivan", "CS", PositionType.FULL_TIME, 40);
        String teacherString = teacher.toString();
        assertTrue(teacherString.contains("Ivanov Ivan"));
        assertTrue(teacherString.contains("CS"));
        assertTrue(teacherString.contains("FULL_TIME"));
        assertTrue(teacherString.contains("id=1"));
        assertTrue(teacherString.contains("weeklyHourLimit=40"));
    }
}
