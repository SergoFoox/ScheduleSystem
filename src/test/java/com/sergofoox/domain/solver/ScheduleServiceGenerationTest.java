package com.sergofoox.domain.solver;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.plan.CoursePlan;
import com.sergofoox.domain.plan.Periodicity;
import com.sergofoox.domain.plan.RoomType;
import com.sergofoox.domain.room.Room;
import com.sergofoox.domain.subject.LessonType;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.teacher.PositionType;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.timeslot.Timeslot;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleServiceGenerationTest {

    @Test
    void convertsEightLectureAndEightPracticeHoursToOnePairPerWeekAcrossOddAndEvenWeeks() {
        CoursePlan plan = planWithHours(8, 8, 0);

        List<ScheduleService.GeneratedLessonSpec> specs = ScheduleService.buildLessonSpecs(plan);

        assertEquals(2, specs.size());
        assertSpec(specs.get(0), LessonType.LECTURE, Periodicity.ODD_WEEKS, 1);
        assertSpec(specs.get(1), LessonType.PRACTICE, Periodicity.EVEN_WEEKS, 1);
    }

    @Test
    void alternatesSingleBiWeeklyLessonsAcrossPlansForSameGroup() {
        Group group = new Group("KB-41", 25, 4, "CS");
        CoursePlan firstPlan = planWithHours(8, 0, 0);
        CoursePlan secondPlan = planWithHours(8, 0, 0);
        firstPlan.setGroup(group);
        secondPlan.setGroup(group);
        ScheduleService.BiWeeklyGenerationState state = new ScheduleService.BiWeeklyGenerationState();

        List<ScheduleService.GeneratedLessonSpec> firstSpecs = ScheduleService.buildLessonSpecs(firstPlan, state);
        List<ScheduleService.GeneratedLessonSpec> secondSpecs = ScheduleService.buildLessonSpecs(secondPlan, state);

        assertEquals(1, firstSpecs.size());
        assertEquals(1, secondSpecs.size());
        assertSpec(firstSpecs.get(0), LessonType.LECTURE, Periodicity.ODD_WEEKS, 1);
        assertSpec(secondSpecs.get(0), LessonType.LECTURE, Periodicity.EVEN_WEEKS, 1);
    }

    @Test
    void keepsSixteenLectureAndSixteenPracticeHoursAsTwoWeeklyPairs() {
        CoursePlan plan = planWithHours(16, 16, 0);

        List<ScheduleService.GeneratedLessonSpec> specs = ScheduleService.buildLessonSpecs(plan);

        assertEquals(2, specs.size());
        assertSpec(specs.get(0), LessonType.LECTURE, Periodicity.WEEKLY, 1);
        assertSpec(specs.get(1), LessonType.PRACTICE, Periodicity.WEEKLY, 1);
    }

    @Test
    void generatesLecturePracticeAndLabLessonsFromPlannedHours() {
        CoursePlan plan = planWithHours(16, 16, 16);

        List<ScheduleService.GeneratedLessonSpec> specs = ScheduleService.buildLessonSpecs(plan);

        assertEquals(3, specs.size());
        assertSpec(specs.get(0), LessonType.LECTURE, Periodicity.WEEKLY, 1);
        assertSpec(specs.get(1), LessonType.PRACTICE, Periodicity.WEEKLY, 1);
        assertSpec(specs.get(2), LessonType.LABORATORY, Periodicity.WEEKLY, 1);
    }

    @Test
    void addsWeeklyAndBiWeeklyLessonsForTwentyFourHours() {
        CoursePlan plan = planWithHours(24, 0, 0);

        List<ScheduleService.GeneratedLessonSpec> specs = ScheduleService.buildLessonSpecs(plan);

        assertEquals(2, specs.size());
        assertSpec(specs.get(0), LessonType.LECTURE, Periodicity.WEEKLY, 1);
        assertSpec(specs.get(1), LessonType.LECTURE, Periodicity.ODD_WEEKS, 2);
    }

    @Test
    void finalCleanupUnschedulesOneLessonFromVisibleGroupConflict() {
        Subject math = subject(1L, "Math");
        Subject physics = subject(2L, "Physics");
        Teacher firstTeacher = teacher(1L);
        Teacher secondTeacher = teacher(2L);
        Group group = group(1L);
        CoursePlan mathPlan = coursePlan(1L, math, firstTeacher, group);
        CoursePlan physicsPlan = coursePlan(2L, physics, secondTeacher, group);
        Timeslot slot = timeslot(1L);
        Room firstRoom = room(1L);
        Room secondRoom = room(2L);

        Lesson first = lesson(1L, math, firstTeacher, group, mathPlan, slot, firstRoom, Periodicity.WEEKLY);
        Lesson second = lesson(2L, physics, secondTeacher, group, physicsPlan, slot, secondRoom, Periodicity.WEEKLY);

        Set<Long> conflicts = ScheduleService.findFinalConflictLessonIds(List.of(first, second));

        assertEquals(Set.of(2L), conflicts);
    }

    @Test
    void finalCleanupUnschedulesSameSubjectOddEvenLessonsInOneCell() {
        Subject subject = subject(1L, "Math");
        Teacher teacher = teacher(1L);
        Group group = group(1L);
        CoursePlan plan = coursePlan(1L, subject, teacher, group);
        Timeslot slot = timeslot(1L);
        Room room = room(1L);

        Lesson odd = lesson(1L, subject, teacher, group, plan, slot, room, Periodicity.ODD_WEEKS);
        Lesson even = lesson(2L, subject, teacher, group, plan, slot, room, Periodicity.EVEN_WEEKS);

        Set<Long> conflicts = ScheduleService.findFinalConflictLessonIds(List.of(odd, even));

        assertEquals(Set.of(2L), conflicts);
    }

    @Test
    void finalCleanupUnschedulesAlternatingLecturePracticeFromSameCoursePlanOneCell() {
        Subject subject = subject(1L, "Math");
        Teacher teacher = teacher(1L);
        Group group = group(1L);
        CoursePlan plan = coursePlan(1L, subject, teacher, group);
        Timeslot slot = timeslot(1L);
        Room room = room(1L);

        Lesson lecture = lesson(1L, subject, teacher, group, plan, slot, room, Periodicity.ODD_WEEKS);
        Lesson practice = lesson(2L, subject, teacher, group, plan, slot, room, Periodicity.EVEN_WEEKS);
        practice.setLessonType(LessonType.PRACTICE);

        Set<Long> conflicts = ScheduleService.findFinalConflictLessonIds(List.of(lecture, practice));

        assertEquals(Set.of(2L), conflicts);
    }

    @Test
    void finalCleanupAllowsDifferentSubjectsInOddEvenOneCell() {
        Subject math = subject(1L, "Math");
        Subject physics = subject(2L, "Physics");
        Teacher firstTeacher = teacher(1L);
        Teacher secondTeacher = teacher(2L);
        Group firstGroup = group(1L);
        Group secondGroup = group(2L);
        CoursePlan mathPlan = coursePlan(1L, math, firstTeacher, firstGroup);
        CoursePlan physicsPlan = coursePlan(2L, physics, secondTeacher, secondGroup);
        Timeslot slot = timeslot(1L);
        Room room = room(1L);

        Lesson odd = lesson(1L, math, firstTeacher, firstGroup, mathPlan, slot, room, Periodicity.ODD_WEEKS);
        Lesson even = lesson(2L, physics, secondTeacher, secondGroup, physicsPlan, slot, room, Periodicity.EVEN_WEEKS);

        Set<Long> conflicts = ScheduleService.findFinalConflictLessonIds(List.of(odd, even));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void finalCleanupUnschedulesSameSubjectTwiceOnOneDay() {
        Subject subject = subject(1L, "Biology");
        Teacher firstTeacher = teacher(1L);
        Teacher secondTeacher = teacher(2L);
        Group group = group(1L);
        CoursePlan plan = coursePlan(1L, subject, firstTeacher, group);
        Room firstRoom = room(1L);
        Room secondRoom = room(2L);

        Lesson first = lesson(1L, subject, firstTeacher, group, plan, timeslot(1L, 1), firstRoom, Periodicity.WEEKLY);
        Lesson second = lesson(2L, subject, secondTeacher, group, plan, timeslot(2L, 2), secondRoom, Periodicity.WEEKLY);

        Set<Long> conflicts = ScheduleService.findFinalConflictLessonIds(List.of(first, second));

        assertEquals(Set.of(2L), conflicts);
    }

    @Test
    void finalCleanupUnschedulesSameSubjectOddEvenTwiceOnDisplayedDay() {
        Subject subject = subject(1L, "Biology");
        Teacher firstTeacher = teacher(1L);
        Teacher secondTeacher = teacher(2L);
        Group group = group(1L);
        CoursePlan plan = coursePlan(1L, subject, firstTeacher, group);
        Room firstRoom = room(1L);
        Room secondRoom = room(2L);

        Lesson odd = lesson(1L, subject, firstTeacher, group, plan, timeslot(1L, 1), firstRoom, Periodicity.ODD_WEEKS);
        Lesson even = lesson(2L, subject, secondTeacher, group, plan, timeslot(2L, 2), secondRoom, Periodicity.EVEN_WEEKS);

        Set<Long> conflicts = ScheduleService.findFinalConflictLessonIds(List.of(odd, even));

        assertEquals(Set.of(2L), conflicts);
    }

    @Test
    void finalCleanupDoesNotDeleteLessonsToFixInternalGroupWindow() {
        Subject math = subject(1L, "Math");
        Subject history = subject(2L, "History");
        Teacher firstTeacher = teacher(1L);
        Teacher secondTeacher = teacher(2L);
        Group group = group(1L);
        CoursePlan mathPlan = coursePlan(1L, math, firstTeacher, group);
        CoursePlan historyPlan = coursePlan(2L, history, secondTeacher, group);
        Room firstRoom = room(1L);
        Room secondRoom = room(2L);

        Lesson first = lesson(1L, math, firstTeacher, group, mathPlan, timeslot(1L, 1), firstRoom, Periodicity.WEEKLY);
        Lesson second = lesson(2L, history, secondTeacher, group, historyPlan, timeslot(2L, 3), secondRoom, Periodicity.WEEKLY);

        Set<Long> conflicts = ScheduleService.findFinalConflictLessonIds(List.of(first, second));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void finalCleanupDoesNotDeleteFourthPairWhenThirdPairIsInternalWindow() {
        Subject math = subject(1L, "Math");
        Subject history = subject(2L, "History");
        Subject chemistry = subject(3L, "Chemistry");
        Teacher firstTeacher = teacher(1L);
        Teacher secondTeacher = teacher(2L);
        Teacher thirdTeacher = teacher(3L);
        Group group = group(1L);
        CoursePlan mathPlan = coursePlan(1L, math, firstTeacher, group);
        CoursePlan historyPlan = coursePlan(2L, history, secondTeacher, group);
        CoursePlan chemistryPlan = coursePlan(3L, chemistry, thirdTeacher, group);
        Room firstRoom = room(1L);
        Room secondRoom = room(2L);
        Room thirdRoom = room(3L);

        Lesson first = lesson(1L, math, firstTeacher, group, mathPlan, timeslot(1L, 1), firstRoom, Periodicity.WEEKLY);
        Lesson second = lesson(2L, history, secondTeacher, group, historyPlan, timeslot(2L, 2), secondRoom, Periodicity.WEEKLY);
        Lesson fourth = lesson(3L, chemistry, thirdTeacher, group, chemistryPlan, timeslot(4L, 4), thirdRoom, Periodicity.WEEKLY);

        Set<Long> conflicts = ScheduleService.findFinalConflictLessonIds(List.of(first, second, fourth));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void finalCleanupDoesNotDeleteThirdPairWhenSecondPairIsEmpty() {
        Subject math = subject(1L, "Math");
        Teacher teacher = teacher(1L);
        Group group = group(1L);
        CoursePlan plan = coursePlan(1L, math, teacher, group);
        Room room = room(1L);

        Lesson third = lesson(1L, math, teacher, group, plan, timeslot(3L, 3), room, Periodicity.WEEKLY);

        Set<Long> conflicts = ScheduleService.findFinalConflictLessonIds(List.of(third));

        assertTrue(conflicts.isEmpty());
    }

    private CoursePlan planWithHours(int lectureHours, int practiceHours, int labHours) {
        Subject subject = new Subject("Math", "M");
        Teacher teacher = new Teacher("Smith", "CS", PositionType.FULL_TIME);
        Group group = new Group("KB-41", 25, 4, "CS");
        CoursePlan plan = new CoursePlan(
                subject,
                teacher,
                group,
                lectureHours + practiceHours + labHours,
                lectureHours,
                practiceHours,
                labHours,
                1,
                1,
                1,
                RoomType.LECTURE_HALL);
        plan.setLecturePeriodicity(Periodicity.WEEKLY);
        plan.setPracticePeriodicity(Periodicity.WEEKLY);
        plan.setLabPeriodicity(Periodicity.WEEKLY);
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

    private Group group(Long id) {
        Group group = new Group("KB-" + id, 25, 4, "CS");
        group.setId(id);
        return group;
    }

    private CoursePlan coursePlan(Long id, Subject subject, Teacher teacher, Group group) {
        CoursePlan plan = new CoursePlan(subject, teacher, group, 16, 16, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        plan.setId(id);
        return plan;
    }

    private Timeslot timeslot(Long id) {
        return timeslot(id, 1);
    }

    private Timeslot timeslot(Long id, int lessonNumber) {
        LocalTime startTime = LocalTime.of(8, 30).plusMinutes((long) (lessonNumber - 1) * 105);
        Timeslot timeslot = new Timeslot(DayOfWeek.MONDAY, startTime, startTime.plusMinutes(90), lessonNumber);
        timeslot.setId(id);
        return timeslot;
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
                          Timeslot timeslot,
                          Room room,
                          Periodicity periodicity) {
        Lesson lesson = new Lesson(subject, LessonType.LECTURE, teacher, group, plan);
        lesson.setId(id);
        lesson.setTimeslot(timeslot);
        lesson.setRoom(room);
        lesson.setPeriodicity(periodicity);
        lesson.setSplitGroupIndex(1);
        return lesson;
    }

    private void assertSpec(ScheduleService.GeneratedLessonSpec spec,
                            LessonType lessonType,
                            Periodicity periodicity,
                            int splitGroupIndex) {
        assertEquals(lessonType, spec.lessonType());
        assertEquals(periodicity, spec.periodicity());
        assertEquals(splitGroupIndex, spec.splitGroupIndex());
    }
}
