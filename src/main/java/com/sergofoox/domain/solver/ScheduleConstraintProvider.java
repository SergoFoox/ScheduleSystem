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

public class ScheduleConstraintProvider implements ConstraintProvider {

    private static final int REQUIRED_VARIABLE_HARD_WEIGHT = 1_000_000;
    private static final int GROUP_INTERNAL_WINDOW_HARD_WEIGHT = 100_000;

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // HARD: КРИТИЧЕСКИЕ (Нельзя нарушать)
                teacherConflict(constraintFactory),
                groupConflict(constraintFactory),
                splitGroupTimeslotSync(constraintFactory),
                roomConflict(constraintFactory),
                subjectConflict(constraintFactory),
                noDuplicateSubjectsPerDay(constraintFactory),
                teacherUnavailableTimeslot(constraintFactory),
                groupOddWeekInternalWindow(constraintFactory),
                groupEvenWeekInternalWindow(constraintFactory),

                // HARD: СРЕДНИЕ (Обязательно к заполнению)
                requiredVariables(constraintFactory),
                timeslotWeekCompatibility(constraintFactory),

                // SOFT: ПРАВИЛА ИЗ ТЗ И РАСПРЕДЕЛЕНИЕ
                assignedTeacherRoom(constraintFactory),
                roomTypeCompatibility(constraintFactory),
                roomCapacity(constraintFactory),
                spreadRooms(constraintFactory),
                teacherRoomStability(constraintFactory),
                groupWindow(constraintFactory),
                teacherWindow(constraintFactory),
                teacherPreferredTimeslot(constraintFactory),
                groupDayStartBalance(constraintFactory),
                loadBalance(constraintFactory),
                compactBiWeekly(constraintFactory)
        };
    }

    // --- HARD CONSTRAINTS ---

    Constraint subjectConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getSubject),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> samePhysicalSlot(l1, l2)
                        && weeksOverlap(l1, l2)
                        && !sameSplitGroupLesson(l1, l2))
                .penalize(HardSoftScore.ofHard(10000))
                .asConstraint("Subject conflict");
    }

    Constraint noDuplicateSubjectsPerDay(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(Lesson::getSubject),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()))
                .filter((l1, l2) -> {
                    if (l1.getTimeslot() == null || l2.getTimeslot() == null) return false;
                    if (!weeksOverlap(l1, l2)) return false;
                    // Если это разные подгруппы одного плана - это нормально
                    if (sameSplitGroupLesson(l1, l2)) return false;
                    // Если это разные номера пар в один день - штрафуем
                    return !l1.getTimeslot().getLessonNumber().equals(l2.getTimeslot().getLessonNumber());
                })
                .penalize(HardSoftScore.ofHard(5000))
                .asConstraint("No duplicate subjects per day");
    }

    // Используем ссылки на методы для корректного отслеживания изменений движком
    Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(this::teacherId),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> teacherId(l1) != null && samePhysicalSlot(l1, l2) && weeksOverlap(l1, l2))
                .penalize(HardSoftScore.ofHard(10000))
                .asConstraint("Teacher conflict");
    }

    Constraint teacherUnavailableTimeslot(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> hasTeacherAvailability(lesson, AvailabilityStatus.UNAVAILABLE))
                .penalize(HardSoftScore.ofHard(10000))
                .asConstraint("Teacher unavailable timeslot");
    }

    Constraint groupOddWeekInternalWindow(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && countsInOddWeek(lesson) && groupDayKey(lesson) != null)
                .groupBy(this::groupDayKey,
                        ConstraintCollectors.<Lesson, Integer>min(lesson -> lesson.getTimeslot().getLessonNumber()),
                        ConstraintCollectors.<Lesson, Integer>max(lesson -> lesson.getTimeslot().getLessonNumber()),
                        ConstraintCollectors.<Lesson>countDistinct(lesson -> lesson.getTimeslot().getLessonNumber()))
                .filter((key, minLesson, maxLesson, lessonCount) -> internalWindowCount(minLesson, maxLesson, lessonCount) > 0)
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
                .filter((key, minLesson, maxLesson, lessonCount) -> internalWindowCount(minLesson, maxLesson, lessonCount) > 0)
                .penalize(HardSoftScore.ofHard(GROUP_INTERNAL_WINDOW_HARD_WEIGHT),
                        (key, minLesson, maxLesson, lessonCount) -> internalWindowCount(minLesson, maxLesson, lessonCount))
                .asConstraint("Group even week internal window");
    }

    Constraint groupConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> samePhysicalSlot(l1, l2)
                        && weeksOverlap(l1, l2)
                        && !sameSplitGroupLesson(l1, l2))
                .penalize(HardSoftScore.ofHard(10000))
                .asConstraint("Group conflict");
    }

    Constraint splitGroupTimeslotSync(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(l -> l.getCoursePlan() == null ? null : l.getCoursePlan().getId()),
                        Joiners.equal(Lesson::getLessonType),
                        Joiners.equal(Lesson::getSplitGroupIndex))
                .filter((l1, l2) -> sameSplitGroupLesson(l1, l2) && !samePhysicalSlot(l1, l2))
                .penalize(HardSoftScore.ofHard(10000))
                .asConstraint("Split group timeslot sync");
    }

    Constraint roomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getRoom),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> l1.getRoom() != null && samePhysicalSlot(l1, l2) && weeksOverlap(l1, l2))
                .penalize(HardSoftScore.ofHard(10000))
                .asConstraint("Room conflict");
    }

    Constraint assignedTeacherRoom(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getRoom() != null
                        && lesson.getTeacher() != null
                        && lesson.getTeacher().getAssignedRoom() != null
                        && !sameId(lesson.getRoom().getId(), lesson.getTeacher().getAssignedRoom().getId()))
                .penalize(HardSoftScore.ofSoft(100))
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
                .penalize(HardSoftScore.ofHard(500))
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
                .filter(lesson -> lesson.getRoom() != null && lesson.getCoursePlan() != null &&
                        lesson.getRoom().getType() != lesson.getCoursePlan().getRequiredRoomType())
                .penalize(HardSoftScore.ofSoft(50))
                .asConstraint("Room type incompatibility");
    }

    Constraint roomCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getRoom() != null &&
                        lesson.getGroup().getSize() > lesson.getRoom().getCapacity())
                .penalize(HardSoftScore.ofSoft(20))
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
                .reward(HardSoftScore.ofSoft(20))
                .asConstraint("Teacher preferred timeslot");
    }

    Constraint groupDayStartBalance(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null && groupDayKey(lesson) != null)
                .groupBy(this::groupDayKey,
                        ConstraintCollectors.<Lesson, Integer>min(lesson -> lesson.getTimeslot().getLessonNumber()))
                .filter((key, firstLessonNumber) -> firstLessonNumber != null && firstLessonNumber > 1)
                .penalize(HardSoftScore.ofSoft(25), (key, firstLessonNumber) -> firstLessonNumber - 1)
                .asConstraint("Group day starts late");
    }

    Constraint loadBalance(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(l -> l.getTimeslot() != null)
                .groupBy(Lesson::getGroup,
                        l -> l.getTimeslot().getDayOfWeek(),
                        Lesson::getPeriodicity,
                        ConstraintCollectors.count())
                .filter((group, day, periodicity, count) -> count > 4)
                .penalize(HardSoftScore.ofSoft(10))
                .asConstraint("Too many lessons per day");
    }

    Constraint teacherRoomStability(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(this::teacherId),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()))
                .filter((l1, l2) -> {
                    if (teacherId(l1) == null) return false;
                    if (l1.getRoom() == null || l2.getRoom() == null || l1.getTimeslot() == null || l2.getTimeslot() == null) return false;
                    if (!weeksOverlap(l1, l2)) return false;
                    return !sameId(l1.getRoom().getId(), l2.getRoom().getId());
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Teacher room stability");
    }

    Constraint compactBiWeekly(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getRoom),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> samePhysicalSlot(l1, l2) && ((l1.getPeriodicity() == Periodicity.ODD_WEEKS && l2.getPeriodicity() == Periodicity.EVEN_WEEKS)
                        || (l1.getPeriodicity() == Periodicity.EVEN_WEEKS && l2.getPeriodicity() == Periodicity.ODD_WEEKS)))
                .reward(HardSoftScore.ofSoft(20))
                .asConstraint("Compact bi-weekly slots");
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

    private boolean sameId(Long firstId, Long secondId) {
        return firstId != null && firstId.equals(secondId);
    }

    private Long teacherId(Lesson lesson) {
        return lesson.getTeacher() != null ? lesson.getTeacher().getId() : null;
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
                .anyMatch(item -> item != null
                        && item.getStatus() == status
                        && item.getDayOfWeek() == lesson.getTimeslot().getDayOfWeek()
                        && lesson.getTimeslot().getLessonNumber().equals(item.getLessonNumber()));
    }

    private boolean countsInOddWeek(Lesson lesson) {
        Periodicity periodicity = effectivePeriodicity(lesson);
        return periodicity == Periodicity.WEEKLY || periodicity == Periodicity.ODD_WEEKS;
    }

    private boolean countsInEvenWeek(Lesson lesson) {
        Periodicity periodicity = effectivePeriodicity(lesson);
        return periodicity == Periodicity.WEEKLY || periodicity == Periodicity.EVEN_WEEKS;
    }

    private GroupDayKey groupDayKey(Lesson lesson) {
        if (lesson.getGroup() == null || lesson.getGroup().getId() == null || lesson.getTimeslot() == null) {
            return null;
        }
        return new GroupDayKey(lesson.getGroup().getId(), lesson.getTimeslot().getDayOfWeek());
    }

    private int internalWindowCount(Integer minLesson, Integer maxLesson, int lessonCount) {
        if (minLesson == null || maxLesson == null || lessonCount <= 1) {
            return 0;
        }
        return Math.max(0, (maxLesson - minLesson + 1) - lessonCount);
    }

    private Periodicity effectivePeriodicity(Lesson lesson) {
        if (lesson.getTimeslot() != null && lesson.getTimeslot().getWeekParity() != Periodicity.WEEKLY) {
            return lesson.getTimeslot().getWeekParity();
        }
        return lesson.getPeriodicity();
    }

    private boolean isTimeslotCompatible(Lesson lesson) {
        Periodicity slotParity = lesson.getTimeslot().getWeekParity();
        return slotParity == Periodicity.WEEKLY || slotParity == lesson.getPeriodicity();
    }

    private record GroupDayKey(Long groupId, DayOfWeek dayOfWeek) {
    }

}
