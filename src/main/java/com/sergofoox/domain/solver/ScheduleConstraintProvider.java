package com.sergofoox.domain.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.plan.Periodicity;
import org.jspecify.annotations.NonNull;

import java.time.Duration;

public class ScheduleConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // HARD: КРИТИЧЕСКИЕ (Нельзя нарушать)
                teacherConflict(constraintFactory),
                groupConflict(constraintFactory),
                roomConflict(constraintFactory),
                subjectConflict(constraintFactory),
                
                // HARD: СРЕДНИЕ (Обязательно к заполнению)
                requiredVariables(constraintFactory),
                timeslotWeekCompatibility(constraintFactory),
                
                // SOFT: ПРАВИЛА ИЗ ТЗ И РАСПРЕДЕЛЕНИЕ
                roomTypeCompatibility(constraintFactory),
                roomCapacity(constraintFactory),
                spreadRooms(constraintFactory), // НОВОЕ: Равномерное распределение по комнатам
                teacherRoomStability(constraintFactory),
                groupWindow(constraintFactory),
                teacherWindow(constraintFactory),
                loadBalance(constraintFactory),
                compactBiWeekly(constraintFactory)
        };
    }

    // --- HARD CONSTRAINTS ---

    // Используем ссылки на методы для корректного отслеживания изменений движком
    Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> samePhysicalSlot(l1, l2) && weeksOverlap(l1, l2))
                .penalize(HardSoftScore.ofHard(1000))
                .asConstraint("Teacher conflict");
    }

    Constraint groupConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> {
                    if (!samePhysicalSlot(l1, l2) || !weeksOverlap(l1, l2)) return false;
                    if (l1.getSubgroup() != 0 && l2.getSubgroup() != 0 && !l1.getSubgroup().equals(l2.getSubgroup())) return false;
                    return true;
                })
                .penalize(HardSoftScore.ofHard(1000))
                .asConstraint("Group conflict");
    }

    Constraint roomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getRoom),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> l1.getRoom() != null && samePhysicalSlot(l1, l2) && weeksOverlap(l1, l2))
                .penalize(HardSoftScore.ofHard(1000))
                .asConstraint("Room conflict");
    }

    Constraint subjectConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getSubject),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> samePhysicalSlot(l1, l2)
                        && weeksOverlap(l1, l2)
                        && !sameSplitGroupLesson(l1, l2))
                .penalize(HardSoftScore.ofSoft(100))
                .asConstraint("Subject conflict");
    }

    Constraint requiredVariables(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() == null || lesson.getRoom() == null)
                .penalize(HardSoftScore.ofHard(100))
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
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()))
                .filter((l1, l2) -> {
                    if (l1.getTimeslot() == null || l2.getTimeslot() == null) return false;
                    if (!weeksOverlap(l1, l2)) return false;
                    Duration between = Duration.between(l1.getTimeslot().getEndTime(), l2.getTimeslot().getStartTime());
                    return !between.isNegative() && between.toMinutes() > 15;
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Teacher window");
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
                Joiners.equal(Lesson::getTeacher),
                Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()))
                .filter((l1, l2) -> {
                    if (l1.getRoom() == null || l2.getRoom() == null || l1.getTimeslot() == null || l2.getTimeslot() == null) return false;
                    if (!weeksOverlap(l1, l2)) return false;
                    return l1.getRoom() != l2.getRoom();
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
        return l1.getGroup().equals(l2.getGroup())
                && l1.getCoursePlan().equals(l2.getCoursePlan())
                && l1.getLessonType() == l2.getLessonType()
                && l1.getSplitGroupIndex().equals(l2.getSplitGroupIndex())
                && l1.getSubgroup() > 0
                && l2.getSubgroup() > 0
                && !l1.getSubgroup().equals(l2.getSubgroup());
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

}
