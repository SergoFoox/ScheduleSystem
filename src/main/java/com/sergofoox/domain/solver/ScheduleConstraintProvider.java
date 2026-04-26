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
                
                // HARD: СРЕДНИЕ (Обязательно к заполнению)
                requiredVariables(constraintFactory),
                
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
                        Joiners.equal(Lesson::getTimeslot))
                .filter((l1, l2) -> l1.getTimeslot() != null && (l1.getPeriodicity() == Periodicity.WEEKLY 
                        || l2.getPeriodicity() == Periodicity.WEEKLY 
                        || l1.getPeriodicity() == l2.getPeriodicity()))
                .penalize(HardSoftScore.ofHard(1000))
                .asConstraint("Teacher conflict");
    }

    Constraint groupConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(Lesson::getTimeslot))
                .filter((l1, l2) -> {
                    if (l1.getTimeslot() == null) return false;
                    boolean weekOverlap = l1.getPeriodicity() == Periodicity.WEEKLY 
                                       || l2.getPeriodicity() == Periodicity.WEEKLY 
                                       || l1.getPeriodicity() == l2.getPeriodicity();
                    if (!weekOverlap) return false;
                    if (l1.getSubgroup() != 0 && l2.getSubgroup() != 0 && !l1.getSubgroup().equals(l2.getSubgroup())) return false;
                    return true;
                })
                .penalize(HardSoftScore.ofHard(1000))
                .asConstraint("Group conflict");
    }

    Constraint roomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getRoom),
                        Joiners.equal(Lesson::getTimeslot))
                .filter((l1, l2) -> {
                    if (l1.getRoom() == null || l1.getTimeslot() == null) return false;
                    return l1.getPeriodicity() == Periodicity.WEEKLY 
                        || l2.getPeriodicity() == Periodicity.WEEKLY 
                        || l1.getPeriodicity() == l2.getPeriodicity();
                })
                .penalize(HardSoftScore.ofHard(1000))
                .asConstraint("Room conflict");
    }

    Constraint requiredVariables(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() == null || lesson.getRoom() == null)
                .penalize(HardSoftScore.ofHard(100))
                .asConstraint("Required variables");
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
                        Joiners.equal(l -> l.getTimeslot().getDayOfWeek()))
                .filter((l1, l2) -> {
                    if (l1.getTimeslot() == null || l2.getTimeslot() == null) return false;
                    boolean weekOverlap = l1.getPeriodicity() == Periodicity.WEEKLY 
                                       || l2.getPeriodicity() == Periodicity.WEEKLY 
                                       || l1.getPeriodicity() == l2.getPeriodicity();
                    if (!weekOverlap) return false;
                    Duration between = Duration.between(l1.getTimeslot().getEndTime(), l2.getTimeslot().getStartTime());
                    return !between.isNegative() && between.toMinutes() > 40;
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Group window");
    }

    Constraint teacherWindow(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(l -> l.getTimeslot().getDayOfWeek()))
                .filter((l1, l2) -> {
                    if (l1.getTimeslot() == null || l2.getTimeslot() == null) return false;
                    boolean weekOverlap = l1.getPeriodicity() == Periodicity.WEEKLY 
                                       || l2.getPeriodicity() == Periodicity.WEEKLY 
                                       || l1.getPeriodicity() == l2.getPeriodicity();
                    if (!weekOverlap) return false;
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
                Joiners.equal(l -> l.getTimeslot().getDayOfWeek()))
                .filter((l1, l2) -> {
                    if (l1.getRoom() == null || l2.getRoom() == null || l1.getTimeslot() == null || l2.getTimeslot() == null) return false;
                    boolean weekOverlap = l1.getPeriodicity() == Periodicity.WEEKLY 
                                       || l2.getPeriodicity() == Periodicity.WEEKLY 
                                       || l1.getPeriodicity() == l2.getPeriodicity();
                    if (!weekOverlap) return false;
                    return l1.getRoom() != l2.getRoom();
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Teacher room stability");
    }

    Constraint compactBiWeekly(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                Joiners.equal(Lesson::getTimeslot),
                Joiners.equal(Lesson::getRoom))
                .filter((l1, l2) -> l1.getTimeslot() != null && ((l1.getPeriodicity() == Periodicity.ODD_WEEKS && l2.getPeriodicity() == Periodicity.EVEN_WEEKS)
                                 || (l1.getPeriodicity() == Periodicity.EVEN_WEEKS && l2.getPeriodicity() == Periodicity.ODD_WEEKS)))
                .reward(HardSoftScore.ofSoft(20))
                .asConstraint("Compact bi-weekly slots");
    }
}
