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
}
