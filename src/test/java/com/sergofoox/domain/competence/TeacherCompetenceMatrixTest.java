package com.sergofoox.domain.competence;

import com.sergofoox.entity.Teacher;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.LessonType;
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
