package com.sergofoox.domain.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import com.sergofoox.domain.lesson.Lesson;

public class ScheduleConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints
                teacherConflict(constraintFactory),
                groupConflict(constraintFactory),
                roomConflict(constraintFactory),
                roomCapacity(constraintFactory),
                
                // Soft constraints
                minimizeGaps(constraintFactory)
        };
    }

    // ТЗ 4.1: Відсутність часових накладок для викладача
    Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(Lesson::getTimeslot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher conflict");
    }

    // ТЗ 4.1: Відсутність часових накладок для групи
    Constraint groupConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(Lesson::getTimeslot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Group conflict");
    }

    // ТЗ 4.1: Відсутність часових накладок для аудиторії
    Constraint roomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getRoom),
                        Joiners.equal(Lesson::getTimeslot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Room conflict");
    }

    // ТЗ 4.1: Кількість студентів у групі <= Місткість аудиторії
    Constraint roomCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getRoom() != null && 
                        lesson.getGroup().getSize() > lesson.getRoom().getCapacity())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Room capacity");
    }

    // ТЗ 4.2: Мінімізація «вікон» (спрощена версія для початку)
    Constraint minimizeGaps(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Minimize gaps");
    }
}
