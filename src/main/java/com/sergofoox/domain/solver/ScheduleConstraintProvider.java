package com.sergofoox.domain.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.plan.Periodicity;
import com.sergofoox.domain.teacher.AvailabilityStatus;
import com.sergofoox.domain.teacher.TeacherAvailability;
import org.jspecify.annotations.NonNull;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.List;
import java.util.Set;

public class ScheduleConstraintProvider implements ConstraintProvider {

    private static final int ACADEMIC_HOURS_PER_LESSON = 2;
    private static final int REQUIRED_VARIABLE_HARD_WEIGHT = 1_000_000;
    private static final int CRITICAL_CONFLICT_HARD_WEIGHT = 500_000;
    private static final int GROUP_INTERNAL_WINDOW_HARD_WEIGHT = 100_000;
    private static final int ASSIGNED_TEACHER_ROOM_SOFT_WEIGHT = 8_000;
    private static final int TEACHER_ROOM_STABILITY_SOFT_WEIGHT = 250;
    private static final int TEACHER_PREFERRED_TIMESLOT_SOFT_WEIGHT = 300;
    private static final int GROUP_DAY_OVERLOAD_SOFT_WEIGHT = 50;
    private static final int TEACHER_DAY_OVERLOAD_SOFT_WEIGHT = 50;
    private static final int GROUP_SINGLE_LESSON_DAY_SOFT_WEIGHT = 2000;
    private static final int GROUP_USED_DAY_SOFT_WEIGHT = 50;
    private static final int GROUP_ORPHAN_BIWEEKLY_SOFT_WEIGHT = 8000;
    private static final int COMPACT_GROUP_BIWEEKLY_SOFT_WEIGHT = 5000;

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // HARD: КРИТИЧЕСКИЕ (Нельзя нарушать)
                teacherConflict(constraintFactory),
                groupConflict(constraintFactory),
                splitGroupTimeslotSync(constraintFactory),
                noSameSubjectInAlternatingSlot(constraintFactory),
                roomConflict(constraintFactory),
                subjectConflict(constraintFactory),
                noDuplicateSubjectsPerDay(constraintFactory),
                groupOddWeekInternalWindow(constraintFactory),
                groupEvenWeekInternalWindow(constraintFactory),
                groupOddWeekSecondThirdPairWindow(constraintFactory),
                groupEvenWeekSecondThirdPairWindow(constraintFactory),
                groupSecondThirdPairBiWeeklyCompleteness(constraintFactory),
                teacherUnavailableTimeslot(constraintFactory),
                teacherOddWeekHourLimit(constraintFactory),
                teacherEvenWeekHourLimit(constraintFactory),
                teacherOddWeekWorkingDayLimit(constraintFactory),
                teacherEvenWeekWorkingDayLimit(constraintFactory),
                
                // HARD: СРЕДНИЕ (Обязательно к заполнению)
                requiredVariables(constraintFactory),
                timeslotWeekCompatibility(constraintFactory),
                
                // SOFT: ПРАВИЛА ИЗ ТЗ И РАСПРЕДЕЛЕНИЕ
                assignedTeacherRoom(constraintFactory),
                roomTypeCompatibility(constraintFactory),
                roomCapacity(constraintFactory),
                spreadRooms(constraintFactory),
                teacherRoomStability(constraintFactory),
                teacherWindow(constraintFactory),
                teacherPreferredTimeslot(constraintFactory),
                groupOddWeekSingleLessonDay(constraintFactory),
                groupEvenWeekSingleLessonDay(constraintFactory),
                groupOddWeekUsedDayCount(constraintFactory),
                groupEvenWeekUsedDayCount(constraintFactory),
                groupBiWeeklyOrphanSlot(constraintFactory),
                loadBalance(constraintFactory),
                teacherDailyLoadBalance(constraintFactory),
                compactGroupBiWeekly(constraintFactory),
                compactBiWeekly(constraintFactory)
        };
    }

    // --- HARD CONSTRAINTS ---

    Constraint subjectConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(this::subjectId),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> subjectId(l1) != null
                        && samePhysicalSlot(l1, l2)
                        && !sameSplitGroupLesson(l1, l2))
                .penalize(HardSoftScore.ofHard(CRITICAL_CONFLICT_HARD_WEIGHT))
                .asConstraint("Subject conflict");
    }

    Constraint noDuplicateSubjectsPerDay(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(this::groupId),
                        Joiners.equal(this::subjectId),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()))
                .filter((l1, l2) -> {
                    if (groupId(l1) == null || subjectId(l1) == null) return false;
                    if (l1.getTimeslot() == null || l2.getTimeslot() == null) return false;
                    // Если это разные подгруппы одного плана - это нормально
                    if (sameSplitGroupLesson(l1, l2)) return false;
                    // Если это разные номера пар в один день - штрафуем
                    return true;
                })
                .penalize(HardSoftScore.ofHard(CRITICAL_CONFLICT_HARD_WEIGHT))
                .asConstraint("No duplicate subjects per day");
    }

    Constraint groupOddWeekInternalWindow(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && countsInOddWeek(lesson) && groupDayKey(lesson) != null)
                .groupBy(this::groupDayKey,
                        ConstraintCollectors.<Lesson, Integer>min(lesson -> lesson.getTimeslot().getLessonNumber()),
                        ConstraintCollectors.<Lesson, Integer>max(lesson -> lesson.getTimeslot().getLessonNumber()),
                        ConstraintCollectors.<Lesson>countDistinct(lesson -> lesson.getTimeslot().getLessonNumber()))
                .filter((key, minLesson, maxLesson, lessonCount) -> hasInternalWindow(minLesson, maxLesson, lessonCount))
                .penalize(HardSoftScore.ofHard(GROUP_INTERNAL_WINDOW_HARD_WEIGHT),
                        (key, minLesson, maxLesson, lessonCount) -> internalWindowCount(minLesson, maxLesson, lessonCount))
                .asConstraint("Group odd week internal window");
    }

    Constraint groupEvenWeekInternalWindow(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && countsInEvenWeek(lesson) && groupDayKey(lesson) != null)
                .groupBy(this::groupDayKey,
                        ConstraintCollectors.<Lesson, Integer>min(lesson -> lesson.getTimeslot().getLessonNumber()),
                        ConstraintCollectors.<Lesson, Integer>max(lesson -> lesson.getTimeslot().getLessonNumber()),
                        ConstraintCollectors.<Lesson>countDistinct(lesson -> lesson.getTimeslot().getLessonNumber()))
                .filter((key, minLesson, maxLesson, lessonCount) -> hasInternalWindow(minLesson, maxLesson, lessonCount))
                .penalize(HardSoftScore.ofHard(GROUP_INTERNAL_WINDOW_HARD_WEIGHT),
                        (key, minLesson, maxLesson, lessonCount) -> internalWindowCount(minLesson, maxLesson, lessonCount))
                .asConstraint("Group even week internal window");
    }

    Constraint groupOddWeekSecondThirdPairWindow(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && countsInOddWeek(lesson) && groupDayKey(lesson) != null)
                .groupBy(this::groupDayKey,
                        ConstraintCollectors.<Lesson, Integer>toSet(lesson -> lesson.getTimeslot().getLessonNumber()))
                .filter((key, lessonNumbers) -> secondThirdPairWindowCount(lessonNumbers) > 0)
                .penalize(HardSoftScore.ofHard(GROUP_INTERNAL_WINDOW_HARD_WEIGHT),
                        (key, lessonNumbers) -> secondThirdPairWindowCount(lessonNumbers))
                .asConstraint("Group odd week second-third pair window");
    }

    Constraint groupEvenWeekSecondThirdPairWindow(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && countsInEvenWeek(lesson) && groupDayKey(lesson) != null)
                .groupBy(this::groupDayKey,
                        ConstraintCollectors.<Lesson, Integer>toSet(lesson -> lesson.getTimeslot().getLessonNumber()))
                .filter((key, lessonNumbers) -> secondThirdPairWindowCount(lessonNumbers) > 0)
                .penalize(HardSoftScore.ofHard(GROUP_INTERNAL_WINDOW_HARD_WEIGHT),
                        (key, lessonNumbers) -> secondThirdPairWindowCount(lessonNumbers))
                .asConstraint("Group even week second-third pair window");
    }

    // Используем ссылки на методы для корректного отслеживания изменений движком
    Constraint groupSecondThirdPairBiWeeklyCompleteness(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null
                        && isSecondOrThirdPair(lesson)
                        && groupSlotKey(lesson) != null)
                .groupBy(this::groupSlotKey,
                        ConstraintCollectors.<Lesson, Periodicity>toSet(this::effectivePeriodicity))
                .filter((key, periodicities) -> hasOnlyOneBiWeeklyHalf(periodicities))
                .penalize(HardSoftScore.ofSoft(5000))
                .asConstraint("Group second-third pair bi-weekly completeness");
    }

    Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(this::teacherId),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> teacherId(l1) != null && samePhysicalSlot(l1, l2) && weeksOverlap(l1, l2))
                .penalize(HardSoftScore.ofHard(CRITICAL_CONFLICT_HARD_WEIGHT))
                .asConstraint("Teacher conflict");
    }

    Constraint teacherUnavailableTimeslot(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> hasTeacherAvailability(lesson, AvailabilityStatus.UNAVAILABLE))
                .penalize(HardSoftScore.ofHard(CRITICAL_CONFLICT_HARD_WEIGHT))
                .asConstraint("Teacher unavailable timeslot");
    }

    Constraint teacherOddWeekHourLimit(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && countsInOddWeek(lesson) && teacherLoadKey(lesson) != null)
                .groupBy(this::teacherLoadKey, ConstraintCollectors.sum(lesson -> ACADEMIC_HOURS_PER_LESSON))
                .filter((teacher, hours) -> exceedsWeeklyHourLimit(teacher, hours))
                .penalize(HardSoftScore.ofHard(2000), (teacher, hours) -> hours - teacher.weeklyHourLimit())
                .asConstraint("Teacher odd week hour limit");
    }

    Constraint teacherEvenWeekHourLimit(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && countsInEvenWeek(lesson) && teacherLoadKey(lesson) != null)
                .groupBy(this::teacherLoadKey, ConstraintCollectors.sum(lesson -> ACADEMIC_HOURS_PER_LESSON))
                .filter((teacher, hours) -> exceedsWeeklyHourLimit(teacher, hours))
                .penalize(HardSoftScore.ofHard(2000), (teacher, hours) -> hours - teacher.weeklyHourLimit())
                .asConstraint("Teacher even week hour limit");
    }

    Constraint teacherOddWeekWorkingDayLimit(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && countsInOddWeek(lesson) && teacherLoadKey(lesson) != null)
                .groupBy(this::teacherLoadKey, ConstraintCollectors.countDistinct(lesson -> lesson.getTimeslot().getDayOfWeek()))
                .filter((teacher, days) -> exceedsWorkingDayLimit(teacher, days))
                .penalize(HardSoftScore.ofHard(1000), (teacher, days) -> (int) (days - teacher.maxWorkingDaysPerWeek()))
                .asConstraint("Teacher odd week working day limit");
    }

    Constraint teacherEvenWeekWorkingDayLimit(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && countsInEvenWeek(lesson) && teacherLoadKey(lesson) != null)
                .groupBy(this::teacherLoadKey, ConstraintCollectors.countDistinct(lesson -> lesson.getTimeslot().getDayOfWeek()))
                .filter((teacher, days) -> exceedsWorkingDayLimit(teacher, days))
                .penalize(HardSoftScore.ofHard(1000), (teacher, days) -> (int) (days - teacher.maxWorkingDaysPerWeek()))
                .asConstraint("Teacher even week working day limit");
    }

    Constraint groupConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(this::groupId),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> groupId(l1) != null
                        && samePhysicalSlot(l1, l2)
                        && weeksOverlap(l1, l2)
                        && !sameSplitGroupLesson(l1, l2))
                .penalize(HardSoftScore.ofHard(CRITICAL_CONFLICT_HARD_WEIGHT))
                .asConstraint("Group conflict");
    }

    Constraint splitGroupTimeslotSync(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(this::groupId),
                        Joiners.equal(l -> l.getCoursePlan() == null ? null : l.getCoursePlan().getId()),
                        Joiners.equal(Lesson::getLessonType),
                        Joiners.equal(Lesson::getSplitGroupIndex))
                .filter((l1, l2) -> sameSplitGroupLesson(l1, l2) && !samePhysicalSlot(l1, l2))
                .penalize(HardSoftScore.ofHard(CRITICAL_CONFLICT_HARD_WEIGHT))
                .asConstraint("Split group timeslot sync");
    }

    Constraint noSameSubjectInAlternatingSlot(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(this::groupId),
                        Joiners.equal(this::subjectId),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter(this::isDuplicateAlternatingSubject)
                .penalize(HardSoftScore.ofHard(CRITICAL_CONFLICT_HARD_WEIGHT))
                .asConstraint("No same subject in alternating slot");
    }

    Constraint roomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getRoom),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> l1.getRoom() != null 
                        && l1.getRoom().getType() != com.sergofoox.domain.plan.RoomType.SPORTS_HALL 
                        && samePhysicalSlot(l1, l2) 
                        && weeksOverlap(l1, l2))
                .penalize(HardSoftScore.ofHard(CRITICAL_CONFLICT_HARD_WEIGHT))
                .asConstraint("Room conflict");
    }

    Constraint gymRoomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(l -> l.getRoom() != null 
                        && l.getTimeslot() != null 
                        && l.getRoom().getType() == com.sergofoox.domain.plan.RoomType.SPORTS_HALL)
                .groupBy(Lesson::getRoom,
                        l -> l.getTimeslot().getDayOfWeek(),
                        l -> l.getTimeslot().getLessonNumber(),
                        ConstraintCollectors.toList())
                .filter((room, day, lessonNumber, lessonsInSlot) -> {
                    int maxOverlaps = 0;
                    for (Lesson l1 : lessonsInSlot) {
                        int overlaps = 0;
                        for (Lesson l2 : lessonsInSlot) {
                            if (weeksOverlap(l1, l2)) {
                                overlaps++;
                            }
                        }
                        maxOverlaps = Math.max(maxOverlaps, overlaps);
                    }
                    return maxOverlaps > 2; // Более 2-х пар, которые пересекаются по неделям
                })
                .penalize(HardSoftScore.ofHard(CRITICAL_CONFLICT_HARD_WEIGHT))
                .asConstraint("Gym room conflict");
    }

    Constraint assignedTeacherRoom(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getRoom() != null
                        && lesson.getTeacher() != null
                        && lesson.getTeacher().getAssignedRoom() != null
                        && !sameId(lesson.getRoom().getId(), lesson.getTeacher().getAssignedRoom().getId()))
                .penalize(HardSoftScore.ofSoft(ASSIGNED_TEACHER_ROOM_SOFT_WEIGHT))
                .asConstraint("Teacher assigned room");
    }

    // subjectConflict удален, так как он слишком ограничивает расписание


    Constraint requiredVariables(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() == null || lesson.getRoom() == null)
                .penalize(HardSoftScore.ofHard(REQUIRED_VARIABLE_HARD_WEIGHT))
                .asConstraint("Required variables");
    }

    Constraint timeslotWeekCompatibility(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && !isTimeslotCompatible(lesson))
                .penalize(HardSoftScore.ofHard(CRITICAL_CONFLICT_HARD_WEIGHT))
                .asConstraint("Timeslot week compatibility");
    }

    // --- SOFT CONSTRAINTS ---

    // Поощряем использование разных аудиторий
    Constraint spreadRooms(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(l -> l.getRoom() != null)
                .groupBy(Lesson::getRoom, ConstraintCollectors.count())
                .filter((room, count) -> count > 2) // Если в одной комнате больше 2 пар в день (в среднем)
                .penalize(HardSoftScore.ofSoft(10))
                .asConstraint("Spread lessons across rooms");
    }

    Constraint roomTypeCompatibility(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> {
                    if (lesson.getRoom() == null) return false;
                    com.sergofoox.domain.plan.RoomType expectedType = (lesson.getTeacher() != null && lesson.getTeacher().getAssignedRoom() != null) 
                            ? lesson.getTeacher().getAssignedRoom().getType() 
                            : (lesson.getCoursePlan() != null ? lesson.getCoursePlan().getRequiredRoomType() : null);
                    return expectedType != null && lesson.getRoom().getType() != expectedType;
                })
                .penalize(HardSoftScore.ofHard(CRITICAL_CONFLICT_HARD_WEIGHT))
                .asConstraint("Room type incompatibility");
    }

    Constraint roomCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getRoom() != null && 
                        lesson.getGroup().getSize() > lesson.getRoom().getCapacity())
                .penalize(HardSoftScore.ofHard(CRITICAL_CONFLICT_HARD_WEIGHT))
                .asConstraint("Room capacity");
    }

    Constraint groupWindow(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()))
                .filter((l1, l2) -> {
                    if (l1.getTimeslot() == null || l2.getTimeslot() == null) return false;
                    if (!weeksOverlap(l1, l2)) return false;
                    Duration between = Duration.between(l1.getTimeslot().getEndTime(), l2.getTimeslot().getStartTime());
                    return !between.isNegative() && between.toMinutes() > 40;
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Group window");
    }

    Constraint teacherWindow(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(this::teacherId),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()))
                .filter((l1, l2) -> {
                    if (teacherId(l1) == null) return false;
                    if (l1.getTimeslot() == null || l2.getTimeslot() == null) return false;
                    if (!weeksOverlap(l1, l2)) return false;
                    Duration between = Duration.between(l1.getTimeslot().getEndTime(), l2.getTimeslot().getStartTime());
                    return !between.isNegative() && between.toMinutes() > 15;
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Teacher window");
    }

    Constraint teacherPreferredTimeslot(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> hasTeacherAvailability(lesson, AvailabilityStatus.PREFERRED))
                .reward(HardSoftScore.ofSoft(TEACHER_PREFERRED_TIMESLOT_SOFT_WEIGHT))
                .asConstraint("Teacher preferred timeslot");
    }

    Constraint loadBalance(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(l -> l.getTimeslot() != null)
                .groupBy(Lesson::getGroup, 
                         l -> l.getTimeslot().getDayOfWeek(), 
                         Lesson::getPeriodicity,
                         ConstraintCollectors.count())
                .filter((group, day, periodicity, count) -> count > 4)
                .penalize(HardSoftScore.ofSoft(GROUP_DAY_OVERLOAD_SOFT_WEIGHT), (group, day, periodicity, count) -> (int) (count - 4))
                .asConstraint("Too many lessons per day");
    }

    Constraint teacherDailyLoadBalance(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && teacherId(lesson) != null)
                .groupBy(this::teacherId,
                        lesson -> lesson.getTimeslot().getDayOfWeek(),
                        Lesson::getPeriodicity,
                        ConstraintCollectors.count())
                .filter((teacherId, day, periodicity, count) -> count > 4)
                .penalize(HardSoftScore.ofSoft(TEACHER_DAY_OVERLOAD_SOFT_WEIGHT), (teacherId, day, periodicity, count) -> (int) (count - 4))
                .asConstraint("Teacher daily load balance");
    }

    Constraint groupOddWeekSingleLessonDay(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && countsInOddWeek(lesson) && groupDayKey(lesson) != null)
                .groupBy(this::groupDayKey,
                        ConstraintCollectors.<Lesson>countDistinct(lesson -> lesson.getTimeslot().getLessonNumber()))
                .filter((key, lessonCount) -> lessonCount == 1)
                .penalize(HardSoftScore.ofSoft(GROUP_SINGLE_LESSON_DAY_SOFT_WEIGHT))
                .asConstraint("Group odd week single lesson day");
    }

    Constraint groupEvenWeekSingleLessonDay(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && countsInEvenWeek(lesson) && groupDayKey(lesson) != null)
                .groupBy(this::groupDayKey,
                        ConstraintCollectors.<Lesson>countDistinct(lesson -> lesson.getTimeslot().getLessonNumber()))
                .filter((key, lessonCount) -> lessonCount == 1)
                .penalize(HardSoftScore.ofSoft(GROUP_SINGLE_LESSON_DAY_SOFT_WEIGHT))
                .asConstraint("Group even week single lesson day");
    }

    Constraint groupOddWeekUsedDayCount(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && countsInOddWeek(lesson) && groupDayKey(lesson) != null)
                .groupBy(this::groupDayKey)
                .penalize(HardSoftScore.ofSoft(GROUP_USED_DAY_SOFT_WEIGHT))
                .asConstraint("Group odd week used day count");
    }

    Constraint groupEvenWeekUsedDayCount(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && countsInEvenWeek(lesson) && groupDayKey(lesson) != null)
                .groupBy(this::groupDayKey)
                .penalize(HardSoftScore.ofSoft(GROUP_USED_DAY_SOFT_WEIGHT))
                .asConstraint("Group even week used day count");
    }

    Constraint groupBiWeeklyOrphanSlot(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && isBiWeekly(lesson) && groupSlotKey(lesson) != null)
                .groupBy(this::groupSlotKey,
                        ConstraintCollectors.<Lesson, Periodicity>toSet(this::effectivePeriodicity))
                .filter((key, periodicities) -> periodicities.size() == 1)
                .penalize(HardSoftScore.ofSoft(GROUP_ORPHAN_BIWEEKLY_SOFT_WEIGHT))
                .asConstraint("Group bi-weekly orphan slot");
    }

    Constraint teacherRoomStability(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                Joiners.equal(this::teacherId))
                .filter((l1, l2) -> {
                    if (teacherId(l1) == null) return false;
                    if (l1.getRoom() == null || l2.getRoom() == null) return false;
                    return !sameId(l1.getRoom().getId(), l2.getRoom().getId());
                })
                .penalize(HardSoftScore.ofSoft(TEACHER_ROOM_STABILITY_SOFT_WEIGHT))
                .asConstraint("Teacher room stability");
    }

    Constraint compactBiWeekly(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                Joiners.equal(Lesson::getRoom),
                Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> samePhysicalSlot(l1, l2)
                        && isComplementaryBiWeekly(l1, l2)
                        && !sameGroupSubject(l1, l2))
                .reward(HardSoftScore.ofSoft(20))
                .asConstraint("Compact bi-weekly slots");
    }

    Constraint compactGroupBiWeekly(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                Joiners.equal(this::groupId),
                Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> samePhysicalSlot(l1, l2)
                        && isComplementaryBiWeekly(l1, l2)
                        && !sameGroupSubject(l1, l2)
                        && !sameSplitGroupLesson(l1, l2))
                .reward(HardSoftScore.ofSoft(COMPACT_GROUP_BIWEEKLY_SOFT_WEIGHT))
                .asConstraint("Compact group bi-weekly slots");
    }

    private boolean samePhysicalSlot(Lesson l1, Lesson l2) {
        if (l1.getTimeslot() == null || l2.getTimeslot() == null) return false;
        return l1.getTimeslot().getDayOfWeek() == l2.getTimeslot().getDayOfWeek()
                && l1.getTimeslot().getLessonNumber().equals(l2.getTimeslot().getLessonNumber());
    }

    private boolean weeksOverlap(Lesson l1, Lesson l2) {
        Periodicity p1 = effectivePeriodicity(l1);
        Periodicity p2 = effectivePeriodicity(l2);
        return p1 == Periodicity.WEEKLY || p2 == Periodicity.WEEKLY || p1 == p2;
    }

    private boolean sameSplitGroupLesson(Lesson l1, Lesson l2) {
        if (l1.getGroup() == null || l2.getGroup() == null
                || l1.getCoursePlan() == null || l2.getCoursePlan() == null
                || l1.getSubgroup() == null || l2.getSubgroup() == null
                || l1.getSplitGroupIndex() == null || l2.getSplitGroupIndex() == null) {
            return false;
        }
        return sameId(l1.getGroup().getId(), l2.getGroup().getId())
                && sameId(l1.getCoursePlan().getId(), l2.getCoursePlan().getId())
                && l1.getLessonType() == l2.getLessonType()
                && l1.getSplitGroupIndex().equals(l2.getSplitGroupIndex())
                && l1.getSubgroup() > 0
                && l2.getSubgroup() > 0
                && !l1.getSubgroup().equals(l2.getSubgroup());
    }

    private boolean isDuplicateAlternatingSubject(Lesson l1, Lesson l2) {
        if (!samePhysicalSlot(l1, l2) || !isComplementaryBiWeekly(l1, l2) || !sameGroupSubject(l1, l2)) {
            return false;
        }
        if (sameSplitGroupLesson(l1, l2)) {
            // Если это подгруппы, у которых преподаватели привязаны к одной аудитории, это не дубликат
            boolean t1SameRoom = l1.getTeacher() != null && l1.getTeacher().getAssignedRoom() != null && l1.getRoom() != null && l1.getRoom().getId().equals(l1.getTeacher().getAssignedRoom().getId());
            boolean t2SameRoom = l2.getTeacher() != null && l2.getTeacher().getAssignedRoom() != null && l2.getRoom() != null && l2.getRoom().getId().equals(l2.getTeacher().getAssignedRoom().getId());
            if (t1SameRoom && t2SameRoom) {
                return false;
            }
        }
        return !sameSplitGroupLesson(l1, l2);
    }

    private boolean sameGroupSubject(Lesson l1, Lesson l2) {
        return l1.getGroup() != null
                && l2.getGroup() != null
                && l1.getSubject() != null
                && l2.getSubject() != null
                && sameId(l1.getGroup().getId(), l2.getGroup().getId())
                && sameId(l1.getSubject().getId(), l2.getSubject().getId());
    }

    private boolean isComplementaryBiWeekly(Lesson l1, Lesson l2) {
        Periodicity first = l1.getPeriodicity();
        Periodicity second = l2.getPeriodicity();
        return (first == Periodicity.ODD_WEEKS && second == Periodicity.EVEN_WEEKS)
                || (first == Periodicity.EVEN_WEEKS && second == Periodicity.ODD_WEEKS);
    }

    private boolean sameId(Long firstId, Long secondId) {
        return firstId != null && firstId.equals(secondId);
    }

    private Long teacherId(Lesson lesson) {
        return lesson.getTeacher() != null ? lesson.getTeacher().getId() : null;
    }

    private Long groupId(Lesson lesson) {
        return lesson.getGroup() != null ? lesson.getGroup().getId() : null;
    }

    private Long subjectId(Lesson lesson) {
        return lesson.getSubject() != null ? lesson.getSubject().getId() : null;
    }

    private boolean hasTeacherAvailability(Lesson lesson, AvailabilityStatus status) {
        if (lesson.getTeacher() == null || lesson.getTimeslot() == null) {
            return false;
        }
        List<TeacherAvailability> availability = lesson.getTeacher().getAvailability();
        if (availability == null || availability.isEmpty()) {
            return false;
        }
        return availability.stream()
                .anyMatch(item -> item.getStatus() == status
                        && item.getDayOfWeek() == lesson.getTimeslot().getDayOfWeek()
                        && item.getLessonNumber().equals(lesson.getTimeslot().getLessonNumber()));
    }

    private Periodicity effectivePeriodicity(Lesson lesson) {
        if (lesson.getTimeslot() != null && lesson.getTimeslot().getWeekParity() != Periodicity.WEEKLY) {
            return lesson.getTimeslot().getWeekParity();
        }
        return lesson.getPeriodicity();
    }

    private boolean countsInOddWeek(Lesson lesson) {
        Periodicity periodicity = effectivePeriodicity(lesson);
        return periodicity == Periodicity.WEEKLY || periodicity == Periodicity.ODD_WEEKS;
    }

    private boolean countsInEvenWeek(Lesson lesson) {
        Periodicity periodicity = effectivePeriodicity(lesson);
        return periodicity == Periodicity.WEEKLY || periodicity == Periodicity.EVEN_WEEKS;
    }

    private boolean isBiWeekly(Lesson lesson) {
        Periodicity periodicity = effectivePeriodicity(lesson);
        return periodicity == Periodicity.ODD_WEEKS || periodicity == Periodicity.EVEN_WEEKS;
    }

    private boolean isSecondOrThirdPair(Lesson lesson) {
        Integer lessonNumber = lesson.getTimeslot().getLessonNumber();
        return lessonNumber != null && (lessonNumber == 2 || lessonNumber == 3);
    }

    private boolean hasOnlyOneBiWeeklyHalf(Set<Periodicity> periodicities) {
        if (periodicities.contains(Periodicity.WEEKLY)) {
            return false;
        }
        return periodicities.size() == 1
                && (periodicities.contains(Periodicity.ODD_WEEKS)
                || periodicities.contains(Periodicity.EVEN_WEEKS));
    }

    private boolean exceedsWeeklyHourLimit(TeacherLoadKey teacher, int hours) {
        return teacher != null
                && teacher.weeklyHourLimit() != null
                && hours > teacher.weeklyHourLimit();
    }

    private boolean exceedsWorkingDayLimit(TeacherLoadKey teacher, long days) {
        return teacher != null
                && teacher.maxWorkingDaysPerWeek() != null
                && days > teacher.maxWorkingDaysPerWeek();
    }

    private TeacherLoadKey teacherLoadKey(Lesson lesson) {
        if (lesson.getTeacher() == null || lesson.getTeacher().getId() == null) {
            return null;
        }
        return new TeacherLoadKey(
                lesson.getTeacher().getId(),
                lesson.getTeacher().getWeeklyHourLimit(),
                lesson.getTeacher().getMaxWorkingDaysPerWeek());
    }

    private GroupDayKey groupDayKey(Lesson lesson) {
        if (lesson.getGroup() == null || lesson.getGroup().getId() == null || lesson.getTimeslot() == null) {
            return null;
        }
        return new GroupDayKey(lesson.getGroup().getId(), lesson.getTimeslot().getDayOfWeek());
    }

    private GroupSlotKey groupSlotKey(Lesson lesson) {
        if (lesson.getGroup() == null || lesson.getGroup().getId() == null || lesson.getTimeslot() == null) {
            return null;
        }
        return new GroupSlotKey(
                lesson.getGroup().getId(),
                lesson.getTimeslot().getDayOfWeek(),
                lesson.getTimeslot().getLessonNumber());
    }

    private boolean hasInternalWindow(Integer minLesson, Integer maxLesson, int lessonCount) {
        return internalWindowCount(minLesson, maxLesson, lessonCount) > 0;
    }

    private int internalWindowCount(Integer minLesson, Integer maxLesson, int lessonCount) {
        if (minLesson == null || maxLesson == null || lessonCount <= 1) {
            return 0;
        }
        return Math.max(0, (maxLesson - minLesson + 1) - lessonCount);
    }

    private int secondThirdPairWindowCount(Set<Integer> lessonNumbers) {
        int windowCount = 0;
        if (isInternalWindowAt(lessonNumbers, 2)) {
            windowCount++;
        }
        if (isInternalWindowAt(lessonNumbers, 3)) {
            windowCount++;
        }
        return windowCount;
    }

    private boolean isInternalWindowAt(Set<Integer> lessonNumbers, int lessonNumber) {
        return !lessonNumbers.contains(lessonNumber)
                && lessonNumbers.stream().anyMatch(number -> number < lessonNumber)
                && lessonNumbers.stream().anyMatch(number -> number > lessonNumber);
    }

    private boolean isTimeslotCompatible(Lesson lesson) {
        Periodicity slotParity = lesson.getTimeslot().getWeekParity();
        return slotParity == Periodicity.WEEKLY || slotParity == lesson.getPeriodicity();
    }

    private record TeacherLoadKey(Long teacherId, Integer weeklyHourLimit, Integer maxWorkingDaysPerWeek) {
    }

    private record GroupDayKey(Long groupId, DayOfWeek dayOfWeek) {
    }

    private record GroupSlotKey(Long groupId, DayOfWeek dayOfWeek, Integer lessonNumber) {
    }

}
