package com.sergofoox.domain.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import com.sergofoox.domain.lesson.Lesson;
import java.time.Duration;

public class ScheduleConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // HARD CONSTRAINTS
                teacherConflict(constraintFactory),
                groupConflict(constraintFactory),
                roomConflict(constraintFactory),
                roomCapacity(constraintFactory),
                
                // SOFT CONSTRAINTS
                teacherRoomStability(constraintFactory),
                groupWindow(constraintFactory),
                teacherWindow(constraintFactory),
                loadBalance(constraintFactory)
        };
    }

    // --- HARD CONSTRAINTS ---

    Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(Lesson::getTimeslot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher conflict");
    }

    Constraint groupConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(Lesson::getTimeslot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Group conflict");
    }

    Constraint roomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getRoom),
                        Joiners.equal(Lesson::getTimeslot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Room conflict");
    }

    Constraint roomCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getRoom() != null && 
                        lesson.getGroup().getSize() > lesson.getRoom().getCapacity())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Room capacity");
    }

    // --- SOFT CONSTRAINTS ---

    // ТЗ 4.2: Мінімізація "вікон" для групи
    // Штрафуємо, якщо між двома уроками однієї групи в один день є часовий розрив
    Constraint groupWindow(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(l -> l.getTimeslot().getDayOfWeek()))
                .filter((l1, l2) -> {
                    Duration between = Duration.between(l1.getTimeslot().getEndTime(), l2.getTimeslot().getStartTime());
                    return !between.isNegative() && between.toMinutes() > 40; // Штрафуємо за будь-яке вікно між парами
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Group window");
    }

    // ТЗ 4.2: Мінімізація "вікон" для викладача
    Constraint teacherWindow(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(l -> l.getTimeslot().getDayOfWeek()))
                .filter((l1, l2) -> {
                    Duration between = Duration.between(l1.getTimeslot().getEndTime(), l2.getTimeslot().getStartTime());
                    return !between.isNegative() && between.toMinutes() > 15; // 15 хв - стандартна перерва, більше - вікно
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Teacher window");
    }

    // ТЗ 4.2: Рівномірність навантаження (уникання занадто насичених днів)
    Constraint loadBalance(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .groupBy(l -> l.getGroup(), l -> l.getTimeslot().getDayOfWeek(), ConstraintCollectors.count())
                .filter((group, day, count) -> count > 4) // Більше 4 пар на день - це вже забагато
                .penalize(HardSoftScore.ofSoft(10)) // Вищий штраф для балансу
                .asConstraint("Too many lessons per day");
    }

    // Додатково: Стабільність аудиторії для викладача (зручність)
    Constraint teacherRoomStability(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                Joiners.equal(Lesson::getTeacher),
                Joiners.equal(l -> l.getTimeslot().getDayOfWeek()))
                .filter((l1, l2) -> !l1.getRoom().equals(l2.getRoom()))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Teacher room stability");
    }
}
