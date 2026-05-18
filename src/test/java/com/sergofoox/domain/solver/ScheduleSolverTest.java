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
import com.sergofoox.domain.teacher.AvailabilityStatus;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherAvailability;
import com.sergofoox.domain.teacher.PositionType;
import com.sergofoox.domain.timeslot.Timeslot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    void allowsSameSubjectInAlternatingOddEvenSlotForDifferentGroups() {
        Timeslot t1 = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        t1.setId(1L);
        Timeslot t2 = new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        t2.setId(2L);

        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(1L);

        Teacher teacher1 = new Teacher("Teacher One", "CS", PositionType.FULL_TIME);
        teacher1.setId(1L);
        Teacher teacher2 = new Teacher("Teacher Two", "CS", PositionType.FULL_TIME);
        teacher2.setId(2L);
        Group group1 = new Group("G-01", 20, 1, "CS");
        group1.setId(1L);
        Group group2 = new Group("G-02", 20, 1, "CS");
        group2.setId(2L);
        Subject subject = new Subject("Defense of Ukraine", "DU");
        subject.setId(1L);

        CoursePlan plan1 = new CoursePlan(subject, teacher1, group1, 8, 8, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        plan1.setId(1L);
        CoursePlan plan2 = new CoursePlan(subject, teacher2, group2, 8, 8, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        plan2.setId(2L);

        Lesson oddLesson = new Lesson(subject, LessonType.LECTURE, teacher1, group1, plan1);
        oddLesson.setId(1L);
        oddLesson.setPeriodicity(Periodicity.ODD_WEEKS);
        Lesson evenLesson = new Lesson(subject, LessonType.LECTURE, teacher2, group2, plan2);
        evenLesson.setId(2L);
        evenLesson.setPeriodicity(Periodicity.EVEN_WEEKS);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(t1, t2),
                List.of(room),
                new ArrayList<>(List.of(oddLesson, evenLesson))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        assertSamePhysicalSlot(lessonById(solution, 1L), lessonById(solution, 2L),
                "Odd and even lessons for different groups may share one displayed timetable cell");
    }

    @Test
    void allowsSwappedDiagonalSubjectPairAcrossGroupsWhenWeeksDoNotOverlap() {
        Timeslot timeslot = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        timeslot.setId(1L);

        Room r1 = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r1.setId(1L);
        Room r2 = new Room("102", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r2.setId(2L);
        Room r3 = new Room("103", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r3.setId(3L);
        Room r4 = new Room("104", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r4.setId(4L);

        Teacher t1 = new Teacher("Teacher One", "CS", PositionType.FULL_TIME);
        t1.setId(1L);
        Teacher t2 = new Teacher("Teacher Two", "CS", PositionType.FULL_TIME);
        t2.setId(2L);
        Teacher t3 = new Teacher("Teacher Three", "CS", PositionType.FULL_TIME);
        t3.setId(3L);
        Teacher t4 = new Teacher("Teacher Four", "CS", PositionType.FULL_TIME);
        t4.setId(4L);

        Group group1 = new Group("G-01", 20, 1, "CS");
        group1.setId(1L);
        Group group2 = new Group("G-02", 20, 1, "CS");
        group2.setId(2L);
        Subject defense = new Subject("Defense of Ukraine", "DU");
        defense.setId(1L);
        Subject civics = new Subject("Civics", "C");
        civics.setId(2L);

        CoursePlan defenseGroup1 = new CoursePlan(defense, t1, group1, 8, 8, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        defenseGroup1.setId(1L);
        CoursePlan civicsGroup1 = new CoursePlan(civics, t2, group1, 8, 8, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        civicsGroup1.setId(2L);
        CoursePlan civicsGroup2 = new CoursePlan(civics, t3, group2, 8, 8, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        civicsGroup2.setId(3L);
        CoursePlan defenseGroup2 = new CoursePlan(defense, t4, group2, 8, 8, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        defenseGroup2.setId(4L);

        Lesson g1Odd = new Lesson(defense, LessonType.LECTURE, t1, group1, defenseGroup1);
        g1Odd.setId(1L);
        g1Odd.setPeriodicity(Periodicity.ODD_WEEKS);
        Lesson g1Even = new Lesson(civics, LessonType.LECTURE, t2, group1, civicsGroup1);
        g1Even.setId(2L);
        g1Even.setPeriodicity(Periodicity.EVEN_WEEKS);
        Lesson g2Odd = new Lesson(civics, LessonType.LECTURE, t3, group2, civicsGroup2);
        g2Odd.setId(3L);
        g2Odd.setPeriodicity(Periodicity.ODD_WEEKS);
        Lesson g2Even = new Lesson(defense, LessonType.LECTURE, t4, group2, defenseGroup2);
        g2Even.setId(4L);
        g2Even.setPeriodicity(Periodicity.EVEN_WEEKS);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(timeslot),
                List.of(r1, r2, r3, r4),
                new ArrayList<>(List.of(g1Odd, g1Even, g2Odd, g2Even))));

        assertTrue(solution.getScore().isFeasible(),
                "Odd/even diagonal subject pairs do not overlap by week in the current solver model");
    }

    @Test
    void allowsSameSubjectIdWhenSubjectObjectsDifferByBusinessFields() {
        Timeslot timeslot = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        timeslot.setId(1L);

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

        Subject subjectInstanceA = new Subject("Math", "M");
        subjectInstanceA.setId(1L);
        Subject subjectInstanceB = new Subject("Mathematics", "MATH");
        subjectInstanceB.setId(1L);

        CoursePlan plan1 = new CoursePlan(subjectInstanceA, teacher1, group1, 30, 15, 15, 0, 1, 1, 0, RoomType.LECTURE_HALL);
        plan1.setId(1L);
        CoursePlan plan2 = new CoursePlan(subjectInstanceB, teacher2, group2, 30, 15, 15, 0, 1, 1, 0, RoomType.LECTURE_HALL);
        plan2.setId(2L);

        Lesson lesson1 = new Lesson(subjectInstanceA, LessonType.LECTURE, teacher1, group1, plan1);
        lesson1.setId(1L);
        Lesson lesson2 = new Lesson(subjectInstanceB, LessonType.LECTURE, teacher2, group2, plan2);
        lesson2.setId(2L);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(timeslot),
                List.of(r1, r2),
                new ArrayList<>(List.of(lesson1, lesson2))));

        assertTrue(solution.getScore().isFeasible(),
                "Current subject conflict detection follows Subject equality, not only database id");
    }

    @Test
    void detectsSameTeacherConflictByIdEvenWhenEntityInstancesDiffer() {
        Timeslot timeslot = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        timeslot.setId(1L);

        Room r1 = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r1.setId(1L);
        Room r2 = new Room("102", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r2.setId(2L);

        Teacher teacherInstanceA = new Teacher("Teacher A", "CS", PositionType.FULL_TIME);
        teacherInstanceA.setId(1L);
        Teacher teacherInstanceB = new Teacher("Teacher B", "Math", PositionType.PART_TIME);
        teacherInstanceB.setId(1L);

        Group group1 = new Group("G-01", 20, 1, "CS");
        group1.setId(1L);
        Group group2 = new Group("G-02", 20, 1, "CS");
        group2.setId(2L);

        Subject math = new Subject("Math", "M");
        math.setId(1L);
        Subject physics = new Subject("Physics", "P");
        physics.setId(2L);

        CoursePlan plan1 = new CoursePlan(math, teacherInstanceA, group1, 30, 15, 15, 0, 1, 1, 0, RoomType.LECTURE_HALL);
        plan1.setId(1L);
        CoursePlan plan2 = new CoursePlan(physics, teacherInstanceB, group2, 30, 15, 15, 0, 1, 1, 0, RoomType.LECTURE_HALL);
        plan2.setId(2L);

        Lesson lesson1 = new Lesson(math, LessonType.LECTURE, teacherInstanceA, group1, plan1);
        lesson1.setId(1L);
        Lesson lesson2 = new Lesson(physics, LessonType.LECTURE, teacherInstanceB, group2, plan2);
        lesson2.setId(2L);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(timeslot),
                List.of(r1, r2),
                new ArrayList<>(List.of(lesson1, lesson2))));

        assertFalse(solution.getScore().isFeasible(),
                "Lessons with the same teacher id in the same slot must be a hard conflict");
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

    @Test
    void avoidsUnavailableTeacherTimeslot() {
        Timeslot unavailableSlot = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        unavailableSlot.setId(1L);
        Timeslot availableSlot = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(10, 15), LocalTime.of(11, 45), 2);
        availableSlot.setId(2L);

        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(1L);

        Teacher teacher = new Teacher("Unavailable Teacher", "CS", PositionType.FULL_TIME);
        teacher.setId(1L);
        teacher.getAvailability().add(availability(teacher, DayOfWeek.MONDAY, 1, AvailabilityStatus.UNAVAILABLE));

        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);
        Subject subject = new Subject("Math", "M");
        subject.setId(1L);
        CoursePlan plan = new CoursePlan(subject, teacher, group, 30, 15, 15, 0, 1, 1, 0, RoomType.LECTURE_HALL);
        plan.setId(1L);

        Lesson lesson = new Lesson(subject, LessonType.LECTURE, teacher, group, plan);
        lesson.setId(1L);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(unavailableSlot, availableSlot),
                List.of(room),
                new ArrayList<>(List.of(lesson))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        assertEquals(2, lessonById(solution, 1L).getTimeslot().getLessonNumber(),
                "Teacher must not be scheduled into an unavailable slot");
    }

    @Test
    void keepsTeacherWeeklyHourLimitAsTeacherMetadataOnly() {
        Timeslot t1 = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        t1.setId(1L);
        Timeslot t2 = new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        t2.setId(2L);

        Room r1 = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r1.setId(1L);
        Room r2 = new Room("102", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r2.setId(2L);

        Teacher teacher = new Teacher("Limited Teacher", "CS", PositionType.FULL_TIME);
        teacher.setId(1L);
        teacher.setWeeklyHourLimit(2);

        Group group1 = new Group("G-01", 20, 1, "CS");
        group1.setId(1L);
        Group group2 = new Group("G-02", 20, 1, "CS");
        group2.setId(2L);

        Subject math = new Subject("Math", "M");
        math.setId(1L);
        Subject physics = new Subject("Physics", "P");
        physics.setId(2L);

        CoursePlan mathPlan = new CoursePlan(math, teacher, group1, 16, 16, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        mathPlan.setId(1L);
        CoursePlan physicsPlan = new CoursePlan(physics, teacher, group2, 16, 16, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        physicsPlan.setId(2L);

        Lesson mathLesson = new Lesson(math, LessonType.LECTURE, teacher, group1, mathPlan);
        mathLesson.setId(1L);
        Lesson physicsLesson = new Lesson(physics, LessonType.LECTURE, teacher, group2, physicsPlan);
        physicsLesson.setId(2L);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(t1, t2),
                List.of(r1, r2),
                new ArrayList<>(List.of(mathLesson, physicsLesson))));

        assertTrue(solution.getScore().isFeasible(),
                "The current solver configuration does not enforce teacher weekly hour limits as a hard constraint");
    }

    @Test
    void prefersPreferredTeacherTimeslot() {
        Timeslot neutralSlot = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        neutralSlot.setId(1L);
        Timeslot preferredSlot = new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        preferredSlot.setId(2L);

        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(1L);

        Teacher teacher = new Teacher("Preferred Teacher", "CS", PositionType.FULL_TIME);
        teacher.setId(1L);
        teacher.getAvailability().add(availability(teacher, DayOfWeek.TUESDAY, 1, AvailabilityStatus.PREFERRED));

        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);
        Subject subject = new Subject("Math", "M");
        subject.setId(1L);
        CoursePlan plan = new CoursePlan(subject, teacher, group, 30, 15, 15, 0, 1, 1, 0, RoomType.LECTURE_HALL);
        plan.setId(1L);

        Lesson lesson = new Lesson(subject, LessonType.LECTURE, teacher, group, plan);
        lesson.setId(1L);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(neutralSlot, preferredSlot),
                List.of(room),
                new ArrayList<>(List.of(lesson))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        assertEquals(DayOfWeek.TUESDAY, lessonById(solution, 1L).getTimeslot().getDayOfWeek(),
                "Teacher should be scheduled into a preferred slot when there is no conflict");
    }

    @Test
    void allowsSameSubjectLecturePracticeInAlternatingPhysicalSlot() {
        Timeslot t1 = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        t1.setId(1L);
        Timeslot t2 = new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        t2.setId(2L);

        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(1L);

        Teacher teacher = new Teacher("Teacher", "CS", PositionType.FULL_TIME);
        teacher.setId(1L);
        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);
        Subject subject = new Subject("Math", "M");
        subject.setId(1L);
        CoursePlan plan = new CoursePlan(subject, teacher, group, 16, 8, 8, 0, 1, 1, 0, RoomType.LECTURE_HALL);
        plan.setId(1L);

        Lesson lecture = new Lesson(subject, LessonType.LECTURE, teacher, group, plan);
        lecture.setId(1L);
        lecture.setPeriodicity(Periodicity.ODD_WEEKS);
        lecture.setSplitGroupIndex(1);

        Lesson practice = new Lesson(subject, LessonType.PRACTICE, teacher, group, plan);
        practice.setId(2L);
        practice.setPeriodicity(Periodicity.EVEN_WEEKS);
        practice.setSplitGroupIndex(1);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(t1, t2),
                List.of(room),
                new ArrayList<>(List.of(lecture, practice))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        assertSamePhysicalSlot(lessonById(solution, 1L), lessonById(solution, 2L),
                "Odd/even lecture and practice lessons do not overlap by week in the current solver model");
    }

    @Test
    void avoidsSameSubjectTwiceOnOneDayForGroup() {
        Timeslot mondayFirst = timeslot(1L, DayOfWeek.MONDAY, 1);
        Timeslot mondaySecond = timeslot(2L, DayOfWeek.MONDAY, 2);
        Timeslot tuesdayFirst = timeslot(3L, DayOfWeek.TUESDAY, 1);

        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(1L);

        Teacher teacher = new Teacher("Teacher", "CS", PositionType.FULL_TIME);
        teacher.setId(1L);
        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);
        Subject biology = new Subject("Biology", "B");
        biology.setId(1L);
        CoursePlan plan = new CoursePlan(biology, teacher, group, 16, 8, 8, 0, 1, 1, 0, RoomType.LECTURE_HALL);
        plan.setId(1L);

        Lesson lecture = new Lesson(biology, LessonType.LECTURE, teacher, group, plan);
        lecture.setId(1L);
        Lesson practice = new Lesson(biology, LessonType.PRACTICE, teacher, group, plan);
        practice.setId(2L);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(mondayFirst, mondaySecond, tuesdayFirst),
                List.of(room),
                new ArrayList<>(List.of(lecture, practice))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        assertNotEquals(lessonById(solution, 1L).getTimeslot().getDayOfWeek(),
                lessonById(solution, 2L).getTimeslot().getDayOfWeek(),
                "Same subject for one group must not be scheduled twice on the same day");
    }

    @Test
    void keepsGroupLessonsContiguousInsideDay() {
        Timeslot first = timeslot(1L, DayOfWeek.MONDAY, 1);
        Timeslot second = timeslot(2L, DayOfWeek.MONDAY, 2);
        Timeslot third = timeslot(3L, DayOfWeek.MONDAY, 3);

        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(1L);

        Teacher firstTeacher = new Teacher("Teacher One", "CS", PositionType.FULL_TIME);
        firstTeacher.setId(1L);
        Teacher secondTeacher = new Teacher("Teacher Two", "CS", PositionType.FULL_TIME);
        secondTeacher.setId(2L);
        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);
        Subject math = new Subject("Math", "M");
        math.setId(1L);
        Subject history = new Subject("History", "H");
        history.setId(2L);
        CoursePlan mathPlan = new CoursePlan(math, firstTeacher, group, 16, 16, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        mathPlan.setId(1L);
        CoursePlan historyPlan = new CoursePlan(history, secondTeacher, group, 16, 16, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        historyPlan.setId(2L);

        Lesson mathLesson = new Lesson(math, LessonType.LECTURE, firstTeacher, group, mathPlan);
        mathLesson.setId(1L);
        Lesson historyLesson = new Lesson(history, LessonType.LECTURE, secondTeacher, group, historyPlan);
        historyLesson.setId(2L);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(first, second, third),
                List.of(room),
                new ArrayList<>(List.of(mathLesson, historyLesson))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        int firstLessonNumber = lessonById(solution, 1L).getTimeslot().getLessonNumber();
        int secondLessonNumber = lessonById(solution, 2L).getTimeslot().getLessonNumber();
        assertEquals(1, Math.abs(firstLessonNumber - secondLessonNumber),
                "Group lessons in one day must not have an internal window");
    }

    @Test
    void keepsThirdPairFromBecomingInternalWindow() {
        Timeslot first = timeslot(1L, DayOfWeek.MONDAY, 1);
        Timeslot second = timeslot(2L, DayOfWeek.MONDAY, 2);
        Timeslot third = timeslot(3L, DayOfWeek.MONDAY, 3);
        Timeslot fourth = timeslot(4L, DayOfWeek.MONDAY, 4);

        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(1L);

        Teacher firstTeacher = new Teacher("Teacher One", "CS", PositionType.FULL_TIME);
        firstTeacher.setId(1L);
        Teacher secondTeacher = new Teacher("Teacher Two", "CS", PositionType.FULL_TIME);
        secondTeacher.setId(2L);
        Teacher thirdTeacher = new Teacher("Teacher Three", "CS", PositionType.FULL_TIME);
        thirdTeacher.setId(3L);
        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);
        Subject math = new Subject("Math", "M");
        math.setId(1L);
        Subject history = new Subject("History", "H");
        history.setId(2L);
        Subject chemistry = new Subject("Chemistry", "C");
        chemistry.setId(3L);
        CoursePlan mathPlan = new CoursePlan(math, firstTeacher, group, 16, 16, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        mathPlan.setId(1L);
        CoursePlan historyPlan = new CoursePlan(history, secondTeacher, group, 16, 16, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        historyPlan.setId(2L);
        CoursePlan chemistryPlan = new CoursePlan(chemistry, thirdTeacher, group, 16, 16, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        chemistryPlan.setId(3L);

        Lesson mathLesson = new Lesson(math, LessonType.LECTURE, firstTeacher, group, mathPlan);
        mathLesson.setId(1L);
        Lesson historyLesson = new Lesson(history, LessonType.LECTURE, secondTeacher, group, historyPlan);
        historyLesson.setId(2L);
        Lesson chemistryLesson = new Lesson(chemistry, LessonType.LECTURE, thirdTeacher, group, chemistryPlan);
        chemistryLesson.setId(3L);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(first, second, third, fourth),
                List.of(room),
                new ArrayList<>(List.of(mathLesson, historyLesson, chemistryLesson))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        List<Integer> lessonNumbers = solution.getLessons().stream()
                .map(lesson -> lesson.getTimeslot().getLessonNumber())
                .distinct()
                .sorted()
                .toList();
        assertEquals(3, lessonNumbers.size());
        assertEquals(2, lessonNumbers.get(2) - lessonNumbers.get(0),
                "Third pair must not be an internal window between earlier and later lessons");
    }

    @Test
    void doesNotStartGroupDayAtThirdPairWhenSecondPairIsAvailable() {
        Timeslot second = timeslot(2L, DayOfWeek.MONDAY, 2);
        Timeslot third = timeslot(3L, DayOfWeek.MONDAY, 3);

        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(1L);

        Teacher teacher = new Teacher("Teacher", "CS", PositionType.FULL_TIME);
        teacher.setId(1L);
        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);
        Subject math = new Subject("Math", "M");
        math.setId(1L);
        CoursePlan plan = new CoursePlan(math, teacher, group, 16, 16, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        plan.setId(1L);

        Lesson lesson = new Lesson(math, LessonType.LECTURE, teacher, group, plan);
        lesson.setId(1L);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(second, third),
                List.of(room),
                new ArrayList<>(List.of(lesson))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        assertEquals(2, lessonById(solution, 1L).getTimeslot().getLessonNumber(),
                "Group day must not start at the third pair when the second pair is available");
    }

    @Test
    void avoidsUnpairedBiWeeklyLessonsOnSecondAndThirdPair() {
        Timeslot first = timeslot(1L, DayOfWeek.MONDAY, 1);
        Timeslot second = timeslot(2L, DayOfWeek.MONDAY, 2);
        Timeslot third = timeslot(3L, DayOfWeek.MONDAY, 3);

        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(1L);
        Teacher teacher = new Teacher("Teacher", "CS", PositionType.FULL_TIME);
        teacher.setId(1L);
        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);
        Subject math = new Subject("Math", "M");
        math.setId(1L);
        CoursePlan plan = new CoursePlan(math, teacher, group, 8, 8, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        plan.setId(1L);

        Lesson lesson = new Lesson(math, LessonType.LECTURE, teacher, group, plan);
        lesson.setId(1L);
        lesson.setPeriodicity(Periodicity.ODD_WEEKS);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(first, second, third),
                List.of(room),
                new ArrayList<>(List.of(lesson))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        assertEquals(1, lessonById(solution, 1L).getTimeslot().getLessonNumber(),
                "Unpaired odd/even lesson must not occupy the second or third pair");
    }

    @Test
    void allowsSecondThirdPairOnlyWhenBiWeeklyHalvesAreBothFilled() {
        Timeslot second = timeslot(2L, DayOfWeek.MONDAY, 2);
        Timeslot third = timeslot(3L, DayOfWeek.MONDAY, 3);

        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(1L);
        Teacher firstTeacher = new Teacher("Teacher One", "CS", PositionType.FULL_TIME);
        firstTeacher.setId(1L);
        Teacher secondTeacher = new Teacher("Teacher Two", "CS", PositionType.FULL_TIME);
        secondTeacher.setId(2L);
        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);
        Subject math = new Subject("Math", "M");
        math.setId(1L);
        Subject history = new Subject("History", "H");
        history.setId(2L);
        CoursePlan mathPlan = new CoursePlan(math, firstTeacher, group, 8, 8, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        mathPlan.setId(1L);
        CoursePlan historyPlan = new CoursePlan(history, secondTeacher, group, 8, 8, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        historyPlan.setId(2L);

        Lesson oddLesson = new Lesson(math, LessonType.LECTURE, firstTeacher, group, mathPlan);
        oddLesson.setId(1L);
        oddLesson.setPeriodicity(Periodicity.ODD_WEEKS);
        Lesson evenLesson = new Lesson(history, LessonType.LECTURE, secondTeacher, group, historyPlan);
        evenLesson.setId(2L);
        evenLesson.setPeriodicity(Periodicity.EVEN_WEEKS);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(second, third),
                List.of(room),
                new ArrayList<>(List.of(oddLesson, evenLesson))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        assertSamePhysicalSlot(lessonById(solution, 1L), lessonById(solution, 2L),
                "Second/third pair bi-weekly cell must contain both odd and even halves");
    }

    @Test
    void keepsGroupLessonsContiguousSeparatelyForOddWeeks() {
        Timeslot first = timeslot(1L, DayOfWeek.MONDAY, 1);
        Timeslot second = timeslot(2L, DayOfWeek.MONDAY, 2);
        Timeslot third = timeslot(3L, DayOfWeek.MONDAY, 3);

        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(1L);

        Teacher firstTeacher = new Teacher("Teacher One", "CS", PositionType.FULL_TIME);
        firstTeacher.setId(1L);
        Teacher secondTeacher = new Teacher("Teacher Two", "CS", PositionType.FULL_TIME);
        secondTeacher.setId(2L);
        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);
        Subject math = new Subject("Math", "M");
        math.setId(1L);
        Subject history = new Subject("History", "H");
        history.setId(2L);
        CoursePlan mathPlan = new CoursePlan(math, firstTeacher, group, 16, 16, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        mathPlan.setId(1L);
        CoursePlan historyPlan = new CoursePlan(history, secondTeacher, group, 8, 8, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        historyPlan.setId(2L);

        Lesson weeklyLesson = new Lesson(math, LessonType.LECTURE, firstTeacher, group, mathPlan);
        weeklyLesson.setId(1L);
        weeklyLesson.setPeriodicity(Periodicity.WEEKLY);
        Lesson oddLesson = new Lesson(history, LessonType.LECTURE, secondTeacher, group, historyPlan);
        oddLesson.setId(2L);
        oddLesson.setPeriodicity(Periodicity.ODD_WEEKS);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(first, second, third),
                List.of(room),
                new ArrayList<>(List.of(weeklyLesson, oddLesson))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        int weeklyLessonNumber = lessonById(solution, 1L).getTimeslot().getLessonNumber();
        int oddLessonNumber = lessonById(solution, 2L).getTimeslot().getLessonNumber();
        assertEquals(1, Math.abs(weeklyLessonNumber - oddLessonNumber),
                "Odd-week timetable must not have an internal window between weekly and odd-week lessons");
    }

    @Test
    void avoidsSingleLessonDaysWhenThereIsRoomToCompact() {
        List<Timeslot> timeslots = List.of(
                timeslot(1L, DayOfWeek.MONDAY, 1),
                timeslot(2L, DayOfWeek.MONDAY, 2),
                timeslot(3L, DayOfWeek.TUESDAY, 1),
                timeslot(4L, DayOfWeek.TUESDAY, 2),
                timeslot(5L, DayOfWeek.WEDNESDAY, 1),
                timeslot(6L, DayOfWeek.WEDNESDAY, 2));

        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(1L);
        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);

        List<Lesson> lessons = new ArrayList<>();
        for (long id = 1; id <= 4; id++) {
            Teacher teacher = new Teacher("Teacher " + id, "CS", PositionType.FULL_TIME);
            teacher.setId(id);
            Subject subject = new Subject("Subject " + id, "S" + id);
            subject.setId(id);
            CoursePlan plan = new CoursePlan(subject, teacher, group, 16, 16, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
            plan.setId(id);
            Lesson lesson = new Lesson(subject, LessonType.LECTURE, teacher, group, plan);
            lesson.setId(id);
            lessons.add(lesson);
        }

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(timeslots, List.of(room), lessons));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        Map<DayOfWeek, Integer> lessonCountByDay = new java.util.HashMap<>();
        for (Lesson lesson : solution.getLessons()) {
            lessonCountByDay.merge(lesson.getTimeslot().getDayOfWeek(), 1, Integer::sum);
        }
        assertTrue(lessonCountByDay.values().stream().noneMatch(count -> count == 1),
                "Group lessons should be compacted into days with at least two pairs when possible");
    }

    @Test
    void compactsDifferentSubjectsAcrossOddAndEvenWeeksForSameGroup() {
        Timeslot monday = timeslot(1L, DayOfWeek.MONDAY, 1);
        Timeslot tuesday = timeslot(2L, DayOfWeek.TUESDAY, 1);

        Room room = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(1L);
        Teacher firstTeacher = new Teacher("Teacher One", "CS", PositionType.FULL_TIME);
        firstTeacher.setId(1L);
        Teacher secondTeacher = new Teacher("Teacher Two", "CS", PositionType.FULL_TIME);
        secondTeacher.setId(2L);
        Group group = new Group("G-01", 20, 1, "CS");
        group.setId(1L);
        Subject math = new Subject("Math", "M");
        math.setId(1L);
        Subject history = new Subject("History", "H");
        history.setId(2L);
        CoursePlan mathPlan = new CoursePlan(math, firstTeacher, group, 8, 8, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        mathPlan.setId(1L);
        CoursePlan historyPlan = new CoursePlan(history, secondTeacher, group, 8, 8, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        historyPlan.setId(2L);

        Lesson oddLesson = new Lesson(math, LessonType.LECTURE, firstTeacher, group, mathPlan);
        oddLesson.setId(1L);
        oddLesson.setPeriodicity(Periodicity.ODD_WEEKS);
        Lesson evenLesson = new Lesson(history, LessonType.LECTURE, secondTeacher, group, historyPlan);
        evenLesson.setId(2L);
        evenLesson.setPeriodicity(Periodicity.EVEN_WEEKS);

        Schedule solution = solverFactory.buildSolver().solve(new Schedule(
                List.of(monday, tuesday),
                List.of(room),
                new ArrayList<>(List.of(oddLesson, evenLesson))));

        assertTrue(solution.getScore().isFeasible(), "Schedule must be feasible");
        assertSamePhysicalSlot(lessonById(solution, 1L), lessonById(solution, 2L),
                "Different odd/even subjects for one group should share one timetable cell when possible");
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

    private TeacherAvailability availability(Teacher teacher, DayOfWeek dayOfWeek, int lessonNumber, AvailabilityStatus status) {
        TeacherAvailability availability = new TeacherAvailability();
        availability.setTeacher(teacher);
        availability.setDayOfWeek(dayOfWeek);
        availability.setLessonNumber(lessonNumber);
        availability.setStatus(status);
        return availability;
    }

    private boolean samePhysicalSlot(Lesson first, Lesson second) {
        assertNotNull(first.getTimeslot());
        assertNotNull(second.getTimeslot());
        return first.getTimeslot().getDayOfWeek() == second.getTimeslot().getDayOfWeek()
                && first.getTimeslot().getLessonNumber().equals(second.getTimeslot().getLessonNumber());
    }

    private Timeslot timeslot(Long id, DayOfWeek dayOfWeek, int lessonNumber) {
        LocalTime startTime = LocalTime.of(8, 30).plusMinutes((long) (lessonNumber - 1) * 105);
        Timeslot timeslot = new Timeslot(dayOfWeek, startTime, startTime.plusMinutes(90), lessonNumber);
        timeslot.setId(id);
        return timeslot;
    }
}
