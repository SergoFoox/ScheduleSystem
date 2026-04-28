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
        Timeslot t1 = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        t1.setId(1L);
        Timeslot t2 = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(10, 15), LocalTime.of(11, 45), 2);
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
        CoursePlan plan = new CoursePlan(subject, teacher, group1, 30, 15, 15, 0, 1, 1, 0, RoomType.LECTURE_HALL);
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

    @Test
    void keepsSplitSubgroupPairsTogetherWithoutOverlappingDifferentSubjects() {
        Timeslot t1 = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        t1.setId(1L);
        Timeslot t2 = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(10, 15), LocalTime.of(11, 45), 2);
        t2.setId(2L);

        Room r1 = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r1.setId(1L);
        Room r2 = new Room("102", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r2.setId(2L);

        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);

        Teacher mathTeacher1 = new Teacher("Math One", "CS", PositionType.FULL_TIME);
        mathTeacher1.setId(1L);
        Teacher mathTeacher2 = new Teacher("Math Two", "CS", PositionType.FULL_TIME);
        mathTeacher2.setId(2L);
        Teacher englishTeacher1 = new Teacher("English One", "CS", PositionType.FULL_TIME);
        englishTeacher1.setId(3L);
        Teacher englishTeacher2 = new Teacher("English Two", "CS", PositionType.FULL_TIME);
        englishTeacher2.setId(4L);

        Subject math = new Subject("Math", "M");
        math.setId(1L);
        Subject english = new Subject("English", "E");
        english.setId(2L);

        CoursePlan mathPlan = new CoursePlan(math, mathTeacher1, group, 30, 0, 30, 0, 0, 1, 0, RoomType.LECTURE_HALL);
        mathPlan.setId(1L);
        mathPlan.setSecondTeacher(mathTeacher2);
        CoursePlan englishPlan = new CoursePlan(english, englishTeacher1, group, 30, 0, 30, 0, 0, 1, 0, RoomType.LECTURE_HALL);
        englishPlan.setId(2L);
        englishPlan.setSecondTeacher(englishTeacher2);

        Lesson mathFirst = splitLesson(1L, math, mathTeacher1, group, mathPlan, 1);
        Lesson mathSecond = splitLesson(2L, math, mathTeacher2, group, mathPlan, 2);
        Lesson englishFirst = splitLesson(3L, english, englishTeacher1, group, englishPlan, 1);
        Lesson englishSecond = splitLesson(4L, english, englishTeacher2, group, englishPlan, 2);

        Schedule problem = new Schedule(
                List.of(t1, t2),
                List.of(r1, r2),
                new ArrayList<>(List.of(mathFirst, mathSecond, englishFirst, englishSecond)));

        Schedule solution = solverFactory.buildSolver().solve(problem);

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");

        Lesson solvedMathFirst = lessonById(solution, 1L);
        Lesson solvedMathSecond = lessonById(solution, 2L);
        Lesson solvedEnglishFirst = lessonById(solution, 3L);
        Lesson solvedEnglishSecond = lessonById(solution, 4L);

        assertSamePhysicalSlot(solvedMathFirst, solvedMathSecond, "Split Math lessons must share a slot");
        assertSamePhysicalSlot(solvedEnglishFirst, solvedEnglishSecond, "Split English lessons must share a slot");
        assertFalse(samePhysicalSlot(solvedMathFirst, solvedEnglishFirst), "Different subjects for one group must not overlap");
    }

    @Test
    void avoidsSameSubjectAtSameTimeForDifferentGroups() {
        Timeslot t1 = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        t1.setId(1L);
        Timeslot t2 = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(10, 15), LocalTime.of(11, 45), 2);
        t2.setId(2L);

        Room r1 = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r1.setId(1L);
        Room r2 = new Room("102", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r2.setId(2L);

        Teacher teacher1 = new Teacher("Teacher One", "CS", PositionType.FULL_TIME);
        teacher1.setId(1L);
        Teacher teacher2 = new Teacher("Teacher Two", "CS", PositionType.FULL_TIME);
        teacher2.setId(2L);
        Group group1 = new Group("G-01", 20, 1, "CS");
        group1.setId(1L);
        Group group2 = new Group("G-02", 20, 1, "CS");
        group2.setId(2L);
        Subject subject = new Subject("Ukrainian", "U");
        subject.setId(1L);

        CoursePlan plan1 = new CoursePlan(subject, teacher1, group1, 30, 15, 15, 0, 1, 1, 0, RoomType.LECTURE_HALL);
        plan1.setId(1L);
        CoursePlan plan2 = new CoursePlan(subject, teacher2, group2, 30, 15, 15, 0, 1, 1, 0, RoomType.LECTURE_HALL);
        plan2.setId(2L);

        Lesson lesson1 = new Lesson(subject, LessonType.LECTURE, teacher1, group1, plan1);
        lesson1.setId(1L);
        Lesson lesson2 = new Lesson(subject, LessonType.LECTURE, teacher2, group2, plan2);
        lesson2.setId(2L);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(t1, t2),
                List.of(r1, r2),
                new ArrayList<>(List.of(lesson1, lesson2))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        assertFalse(samePhysicalSlot(lessonById(solution, 1L), lessonById(solution, 2L)),
                "Same subject for different groups must not share a slot");
    }

    @Test
    void usesAssignedTeacherRoom() {
        Timeslot timeslot = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        timeslot.setId(1L);

        Room otherRoom = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        otherRoom.setId(1L);
        Room assignedRoom = new Room("27", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        assignedRoom.setId(2L);

        Teacher teacher = new Teacher("Assigned Teacher", "CS", PositionType.FULL_TIME);
        teacher.setId(1L);
        teacher.setAssignedRoom(assignedRoom);
        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);
        Subject subject = new Subject("Math", "M");
        subject.setId(1L);
        CoursePlan plan = new CoursePlan(subject, teacher, group, 30, 15, 15, 0, 1, 1, 0, RoomType.LECTURE_HALL);
        plan.setId(1L);

        Lesson lesson = new Lesson(subject, LessonType.LECTURE, teacher, group, plan);
        lesson.setId(1L);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(timeslot),
                List.of(otherRoom, assignedRoom),
                new ArrayList<>(List.of(lesson))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        assertSameRoom(assignedRoom, lessonById(solution, 1L).getRoom(), "Teacher must use assigned room");
    }

    private Lesson splitLesson(Long id, Subject subject, Teacher teacher, Group group, CoursePlan plan, int subgroup) {
        Lesson lesson = new Lesson(subject, LessonType.PRACTICE, teacher, group, plan, subgroup);
        lesson.setId(id);
        lesson.setSplitGroupIndex(1);
        lesson.setPeriodicity(Periodicity.WEEKLY);
        return lesson;
    }

    private Lesson lessonById(Schedule schedule, Long id) {
        return schedule.getLessons().stream()
                .filter(lesson -> id.equals(lesson.getId()))
                .findFirst()
                .orElseThrow();
    }

    private void assertSamePhysicalSlot(Lesson first, Lesson second, String message) {
        assertTrue(samePhysicalSlot(first, second), message);
    }

    private void assertSameRoom(Room expected, Room actual, String message) {
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId(), message);
    }

    private boolean samePhysicalSlot(Lesson first, Lesson second) {
        assertNotNull(first.getTimeslot());
        assertNotNull(second.getTimeslot());
        return first.getTimeslot().getDayOfWeek() == second.getTimeslot().getDayOfWeek()
                && first.getTimeslot().getLessonNumber().equals(second.getTimeslot().getLessonNumber());
    }
}
