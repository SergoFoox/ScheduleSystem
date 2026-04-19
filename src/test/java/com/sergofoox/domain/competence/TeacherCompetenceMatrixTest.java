package com.sergofoox.domain.competence;

import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.PositionType;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.LessonType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeacherCompetenceMatrixTest {
    @Test
    void testMatrixCreation() {
        Teacher teacher = new Teacher("Ivanov Ivan", "CS", PositionType.FULL_TIME);
        Subject subject = new Subject("Mathematics", "Math");
        TeacherCompetenceMatrix matrix = new TeacherCompetenceMatrix(teacher, subject, LessonType.LECTURE, Priority.PRIMARY);
        
        assertEquals(teacher, matrix.getTeacher());
        assertEquals(subject, matrix.getSubject());
        assertEquals(LessonType.LECTURE, matrix.getLessonType());
        assertEquals(Priority.PRIMARY, matrix.getPriority());
    }

    @Test
    void testBusinessKeyEquality() {
        Teacher t = new Teacher("Ivan", "CS", PositionType.FULL_TIME);
        Subject s = new Subject("Math", "M");
        TeacherCompetenceMatrix m1 = new TeacherCompetenceMatrix(1L, t, s, LessonType.LECTURE, Priority.PRIMARY);
        TeacherCompetenceMatrix m2 = new TeacherCompetenceMatrix(2L, t, s, LessonType.LECTURE, Priority.SECONDARY);
        
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void testInequality() {
        Teacher t = new Teacher("Ivan", "CS", PositionType.FULL_TIME);
        Subject s1 = new Subject("Math", "M");
        Subject s2 = new Subject("Physics", "P");
        TeacherCompetenceMatrix m1 = new TeacherCompetenceMatrix(t, s1, LessonType.LECTURE, Priority.PRIMARY);
        TeacherCompetenceMatrix m2 = new TeacherCompetenceMatrix(t, s2, LessonType.LECTURE, Priority.PRIMARY);
        
        assertNotEquals(m1, m2);
    }
}
