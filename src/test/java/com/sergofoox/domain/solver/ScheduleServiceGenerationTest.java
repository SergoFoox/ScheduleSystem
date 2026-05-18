package com.sergofoox.domain.solver;

import ai.timefold.solver.core.api.solver.SolverManager;
import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.group.GroupRepository;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.plan.CoursePlan;
import com.sergofoox.domain.plan.CoursePlanRepository;
import com.sergofoox.domain.plan.Periodicity;
import com.sergofoox.domain.plan.RoomType;
import com.sergofoox.domain.room.Room;
import com.sergofoox.domain.room.RoomRepository;
import com.sergofoox.domain.subject.LessonType;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.SubjectRepository;
import com.sergofoox.domain.teacher.PositionType;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.timeslot.TimeslotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceGenerationTest {

    @Mock
    private SolverManager<Schedule, UUID> solverManager;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TimeslotRepository timeslotRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CoursePlanRepository coursePlanRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(
                solverManager,
                teacherRepository,
                groupRepository,
                roomRepository,
                timeslotRepository,
                lessonRepository,
                coursePlanRepository,
                subjectRepository,
                transactionTemplate);
    }

    @Test
    void generatesLessonsFromConfiguredSessionCountsAndPeriodicity() {
        CoursePlan plan = coursePlan(1L, "Math", 1, 2, 1, 1);
        plan.setLecturePeriodicity(Periodicity.WEEKLY);
        plan.setPracticePeriodicity(Periodicity.ODD_WEEKS);
        plan.setLabPeriodicity(Periodicity.EVEN_WEEKS);
        when(coursePlanRepository.findAll()).thenReturn(List.of(plan));

        scheduleService.generateLessonsFromPlans();

        List<Lesson> lessons = capturedSavedLessons();
        assertEquals(4, lessons.size());
        assertLesson(lessons.get(0), plan, LessonType.LECTURE, Periodicity.WEEKLY, 1, 0, plan.getTeacher());
        assertLesson(lessons.get(1), plan, LessonType.LECTURE, Periodicity.WEEKLY, 2, 0, plan.getTeacher());
        assertLesson(lessons.get(2), plan, LessonType.PRACTICE, Periodicity.ODD_WEEKS, 1, 0, plan.getTeacher());
        assertLesson(lessons.get(3), plan, LessonType.LABORATORY, Periodicity.EVEN_WEEKS, 1, 0, plan.getTeacher());
        verify(lessonRepository).deleteAll();
    }

    @Test
    void keepsLowHourWeeklyPlanWeeklyWhenPlanPeriodicityIsWeekly() {
        CoursePlan plan = coursePlan(1L, "Defense", 1, 1, 0, 0);
        plan.setTotalHours(8);
        plan.setLectureHours(8);
        plan.setLecturePeriodicity(Periodicity.WEEKLY);
        when(coursePlanRepository.findAll()).thenReturn(List.of(plan));

        scheduleService.generateLessonsFromPlans();

        List<Lesson> lessons = capturedSavedLessons();
        assertEquals(1, lessons.size());
        assertLesson(lessons.get(0), plan, LessonType.LECTURE, Periodicity.WEEKLY, 1, 0, plan.getTeacher());
    }

    @Test
    void usesExplicitBiWeeklyPeriodicityFromCoursePlan() {
        CoursePlan plan = coursePlan(1L, "History", 1, 1, 1, 0);
        plan.setLecturePeriodicity(Periodicity.ODD_WEEKS);
        plan.setPracticePeriodicity(Periodicity.EVEN_WEEKS);
        when(coursePlanRepository.findAll()).thenReturn(List.of(plan));

        scheduleService.generateLessonsFromPlans();

        List<Lesson> lessons = capturedSavedLessons();
        assertEquals(2, lessons.size());
        assertLesson(lessons.get(0), plan, LessonType.LECTURE, Periodicity.ODD_WEEKS, 1, 0, plan.getTeacher());
        assertLesson(lessons.get(1), plan, LessonType.PRACTICE, Periodicity.EVEN_WEEKS, 1, 0, plan.getTeacher());
    }

    @Test
    void createsOneLessonForEachTeacherWhenPlanHasTwoTeachers() {
        CoursePlan plan = coursePlan(1L, "English", 1, 0, 1, 0);
        Teacher secondTeacher = teacher(2L);
        plan.setSecondTeacher(secondTeacher);
        plan.setPracticePeriodicity(Periodicity.WEEKLY);
        when(coursePlanRepository.findAll()).thenReturn(List.of(plan));

        scheduleService.generateLessonsFromPlans();

        List<Lesson> lessons = capturedSavedLessons();
        assertEquals(2, lessons.size());
        assertLesson(lessons.get(0), plan, LessonType.PRACTICE, Periodicity.WEEKLY, 1, 1, plan.getTeacher());
        assertLesson(lessons.get(1), plan, LessonType.PRACTICE, Periodicity.WEEKLY, 1, 2, secondTeacher);
    }

    @Test
    void courseFilterDeletesAndRegeneratesOnlySelectedCourseGroups() {
        Group firstCourseGroup = group(1L, 1);
        Group secondCourseGroup = group(2L, 2);
        CoursePlan selectedPlan = coursePlan(1L, "Math", 1, 1, 0, 0);
        CoursePlan otherPlan = coursePlan(2L, "Physics", 2, 1, 0, 0);
        selectedPlan.setGroup(firstCourseGroup);
        otherPlan.setGroup(secondCourseGroup);
        when(groupRepository.findAll()).thenReturn(List.of(firstCourseGroup, secondCourseGroup));
        when(coursePlanRepository.findAll()).thenReturn(List.of(selectedPlan, otherPlan));

        scheduleService.generateLessonsFromPlans(1);

        List<Lesson> lessons = capturedSavedLessons();
        assertEquals(1, lessons.size());
        assertSame(firstCourseGroup, lessons.get(0).getGroup());
        verify(lessonRepository).deleteByGroup(firstCourseGroup);
        verify(lessonRepository, never()).deleteByGroup(secondCourseGroup);
        verify(lessonRepository, never()).deleteAll();
    }

    @Test
    void throwsWhenSelectedCourseHasNoGroups() {
        when(groupRepository.findAll()).thenReturn(List.of(group(1L, 1)));

        assertThrows(IllegalArgumentException.class, () -> scheduleService.generateLessonsFromPlans(2));
    }

    @Test
    void throwsWhenPlansHaveNoTeachersAndNothingCanBeGenerated() {
        CoursePlan plan = coursePlan(1L, "Math", 1, 1, 0, 0);
        plan.setTeacher(null);
        plan.setSecondTeacher(null);
        when(coursePlanRepository.findAll()).thenReturn(List.of(plan));

        assertThrows(IllegalStateException.class, () -> scheduleService.generateLessonsFromPlans());
    }

    @Test
    void saveSolutionUnschedulesTeacherConflictLoser() {
        Subject subject = subject(1L, "Math");
        Teacher teacher = teacher(1L);
        Group firstGroup = group(1L, 1);
        Group secondGroup = group(2L, 1);
        CoursePlan firstPlan = coursePlan(1L, subject, teacher, firstGroup);
        CoursePlan secondPlan = coursePlan(2L, subject, teacher, secondGroup);
        com.sergofoox.domain.timeslot.Timeslot timeslot = new com.sergofoox.domain.timeslot.Timeslot(
                DayOfWeek.MONDAY,
                LocalTime.of(8, 30),
                LocalTime.of(10, 0),
                1);
        timeslot.setId(1L);
        Room room = room(1L);
        Lesson first = lesson(1L, subject, teacher, firstGroup, firstPlan, timeslot, room);
        Lesson second = lesson(2L, subject, teacher, secondGroup, secondPlan, timeslot, room);
        Lesson firstDb = lesson(1L, subject, teacher, firstGroup, firstPlan, timeslot, room);
        Lesson secondDb = lesson(2L, subject, teacher, secondGroup, secondPlan, timeslot, room);
        mockTransactionTemplate();
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(firstDb));
        when(lessonRepository.findById(2L)).thenReturn(Optional.of(secondDb));
        when(timeslotRepository.getReferenceById(1L)).thenReturn(timeslot);
        when(roomRepository.getReferenceById(1L)).thenReturn(room);

        scheduleService.saveSolution(new Schedule(List.of(timeslot), List.of(room), List.of(first, second)));

        assertSame(timeslot, firstDb.getTimeslot());
        assertNull(secondDb.getTimeslot());
        assertNull(secondDb.getRoom());
    }

    private List<Lesson> capturedSavedLessons() {
        ArgumentCaptor<Iterable<Lesson>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(lessonRepository).saveAll(captor.capture());
        List<Lesson> lessons = new ArrayList<>();
        captor.getValue().forEach(lessons::add);
        return lessons;
    }

    private void mockTransactionTemplate() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private void assertLesson(Lesson lesson,
                              CoursePlan plan,
                              LessonType lessonType,
                              Periodicity periodicity,
                              int splitGroupIndex,
                              int subgroup,
                              Teacher teacher) {
        assertSame(plan.getSubject(), lesson.getSubject());
        assertSame(plan.getGroup(), lesson.getGroup());
        assertSame(plan, lesson.getCoursePlan());
        assertSame(teacher, lesson.getTeacher());
        assertEquals(lessonType, lesson.getLessonType());
        assertEquals(periodicity, lesson.getPeriodicity());
        assertEquals(splitGroupIndex, lesson.getSplitGroupIndex());
        assertEquals(subgroup, lesson.getSubgroup());
    }

    private CoursePlan coursePlan(Long id, String subjectName, long teacherId, int lectureSessions, int practiceSessions, int labSessions) {
        Subject subject = subject(id, subjectName);
        Teacher teacher = teacher(teacherId);
        Group group = group(id, 1);
        CoursePlan plan = new CoursePlan(
                subject,
                teacher,
                group,
                (lectureSessions + practiceSessions + labSessions) * 16,
                lectureSessions * 16,
                practiceSessions * 16,
                labSessions * 16,
                lectureSessions,
                practiceSessions,
                labSessions,
                RoomType.LECTURE_HALL);
        plan.setId(id);
        plan.setLecturePeriodicity(Periodicity.WEEKLY);
        plan.setPracticePeriodicity(Periodicity.WEEKLY);
        plan.setLabPeriodicity(Periodicity.WEEKLY);
        return plan;
    }

    private CoursePlan coursePlan(Long id, Subject subject, Teacher teacher, Group group) {
        CoursePlan plan = new CoursePlan(subject, teacher, group, 16, 16, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        plan.setId(id);
        return plan;
    }

    private Subject subject(Long id, String name) {
        Subject subject = new Subject(name, name.substring(0, 1));
        subject.setId(id);
        return subject;
    }

    private Teacher teacher(Long id) {
        Teacher teacher = new Teacher("Teacher " + id, "CS", PositionType.FULL_TIME);
        teacher.setId(id);
        return teacher;
    }

    private Group group(Long id, int course) {
        Group group = new Group("G-" + id, 25, course, "CS");
        group.setId(id);
        return group;
    }

    private Room room(Long id) {
        Room room = new Room("Room " + id, 30, "Main", "Projector", RoomType.LECTURE_HALL);
        room.setId(id);
        return room;
    }

    private Lesson lesson(Long id,
                          Subject subject,
                          Teacher teacher,
                          Group group,
                          CoursePlan plan,
                          com.sergofoox.domain.timeslot.Timeslot timeslot,
                          Room room) {
        Lesson lesson = new Lesson(subject, LessonType.LECTURE, teacher, group, plan);
        lesson.setId(id);
        lesson.setTimeslot(timeslot);
        lesson.setRoom(room);
        lesson.setPeriodicity(Periodicity.WEEKLY);
        return lesson;
    }
}
