package com.sergofoox.domain.solver;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import com.sergofoox.config.SolverConfiguration;
import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.plan.CoursePlan;
import com.sergofoox.domain.plan.Periodicity;
import com.sergofoox.domain.room.Room;
import com.sergofoox.domain.plan.RoomType;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.LessonType;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.PositionType;
import com.sergofoox.domain.timeslot.Timeslot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ScheduleSolverTest {

    @Autowired
    private SolverFactory<Schedule> solverFactory;

    @Test
    void solveBasicSchedule() {
        // 1. Створюємо дані (Facts)
        Timeslot t1 = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0));
        t1.setId(1L);
        Timeslot t2 = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(10, 15), LocalTime.of(11, 45));
        t2.setId(2L);
        List<Timeslot> timeslots = List.of(t1, t2);

        Room r1 = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r1.setId(1L);
        Room r2 = new Room("102", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r2.setId(2L);
        List<Room> rooms = List.of(r1, r2);

        Teacher teacher = new Teacher("Dr. Smith", "CS", PositionType.FULL_TIME);
        teacher.setId(1L);
        Group group1 = new Group("G-01", 20, 1, "CS");
        group1.setId(1L);
        Group group2 = new Group("G-02", 20, 1, "CS");
        group2.setId(2L);
        Subject subject = new Subject("Math", "M");
        subject.setId(1L);
        CoursePlan plan = new CoursePlan(subject, group1, 30, 15, 15, 0, 1, 1, 0, RoomType.LECTURE_HALL);
        plan.setId(1L);

        // 2. Створюємо заняття (Entities), які треба спланувати
        List<Lesson> lessons = new ArrayList<>();
        Lesson lesson1 = new Lesson(subject, LessonType.LECTURE, teacher, group1, plan);
        lesson1.setId(1L);
        Lesson lesson2 = new Lesson(subject, LessonType.LECTURE, teacher, group2, plan);
        lesson2.setId(2L);
        
        lessons.add(lesson1);
        lessons.add(lesson2);

        Schedule problem = new Schedule(timeslots, rooms, lessons);

        // 3. Викликаємо Solver
        Solver<Schedule> solver = solverFactory.buildSolver();
        Schedule solution = solver.solve(problem);

        // 4. Перевірка результату
        assertNotNull(solution.getScore());
        assertTrue(solution.getScore().isFeasible(), "Розклад має бути реалістичним (без жорстких конфліктів)");
        
        for (Lesson lesson : solution.getLessons()) {
            assertNotNull(lesson.getTimeslot(), "Кожне заняття повинно мати часовий слот");
            assertNotNull(lesson.getRoom(), "Кожне заняття повинно мати аудиторію");
        }

        // Перевірка, що один вчитель не в двох місцях одночасно
        Lesson l1 = solution.getLessons().get(0);
        Lesson l2 = solution.getLessons().get(1);
        assertNotEquals(l1.getTimeslot(), l2.getTimeslot(), "Вчитель не може вести дві пари одночасно");
    }
}
