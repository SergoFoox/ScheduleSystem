package com.sergofoox.domain.lesson;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.plan.CoursePlan;
import com.sergofoox.domain.room.Room;
import com.sergofoox.domain.plan.RoomType;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.LessonType;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.PositionType;
import com.sergofoox.domain.timeslot.Timeslot;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class LessonTest {

    @Test
    void testLessonCreationAndPlanningVariables() {
        Subject subject = new Subject("Math", "M");
        Teacher teacher = new Teacher("Ivanov", "CS", PositionType.FULL_TIME);
        Group group = new Group("P-11", 25, 1, "CS");
        CoursePlan plan = new CoursePlan();
        
        Lesson lesson = new Lesson(subject, LessonType.LECTURE, teacher, group, plan);
        lesson.setId(1L);

        assertNull(lesson.getTimeslot());
        assertNull(lesson.getRoom());

        Timeslot timeslot = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);

        lesson.setTimeslot(timeslot);
        lesson.setRoom(room);

        assertEquals(timeslot, lesson.getTimeslot());
        assertEquals(room, lesson.getRoom());
        assertEquals("Lesson(1)", lesson.toString());
    }
}
