package com.sergofoox.domain.teacher;

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

    @Test
    void testTeacherAllArgsConstructor() {
        Teacher teacher = new Teacher(1L, "Petrov Petr", "Mathematics", "Part-time");
        assertEquals(1L, teacher.getId());
        assertEquals("Petrov Petr", teacher.getFullName());
        assertEquals("Mathematics", teacher.getDepartment());
        assertEquals("Part-time", teacher.getPositionType());
    }

    @Test
    void testEquality() {
        Teacher t1 = new Teacher(1L, "Ivanov Ivan", "CS", "Full");
        Teacher t2 = new Teacher(1L, "Ivanov Ivan", "CS", "Full");
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void testToString() {
        Teacher teacher = new Teacher(1L, "Ivanov Ivan", "CS", "Full");
        String teacherString = teacher.toString();
        assertTrue(teacherString.contains("Ivanov Ivan"));
        assertTrue(teacherString.contains("CS"));
        assertTrue(teacherString.contains("Full"));
        assertTrue(teacherString.contains("id=1"));
    }
}
