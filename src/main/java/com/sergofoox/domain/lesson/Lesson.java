package com.sergofoox.domain.lesson;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.plan.CoursePlan;
import com.sergofoox.domain.room.Room;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.LessonType;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.timeslot.Timeslot;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@PlanningEntity
@Entity
public class Lesson {

    @PlanningId
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private Subject subject;

    @NotNull
    @Enumerated(EnumType.STRING)
    private LessonType lessonType;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private Teacher teacher;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private Group group;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private CoursePlan coursePlan;

    @PlanningVariable
    @ManyToOne(fetch = FetchType.LAZY)
    private Timeslot timeslot;

    @PlanningVariable
    @ManyToOne(fetch = FetchType.LAZY)
    private Room room;

    @NotNull
    @Enumerated(EnumType.STRING)
    private com.sergofoox.domain.plan.Periodicity periodicity = com.sergofoox.domain.plan.Periodicity.WEEKLY;

    private Integer subgroup = 0; // 0 = whole group, 1 = first, 2 = second

    private Integer splitGroupIndex = 0;

    @PlanningPin
    @Transient
    private boolean pinned;

    public Lesson() {
    }

    public Lesson(Subject subject, LessonType lessonType, Teacher teacher, Group group, CoursePlan coursePlan) {
        this.subject = subject;
        this.lessonType = lessonType;
        this.teacher = teacher;
        this.group = group;
        this.coursePlan = coursePlan;
    }

    public Lesson(Subject subject, LessonType lessonType, Teacher teacher, Group group, CoursePlan coursePlan, Integer subgroup) {
        this.subject = subject;
        this.lessonType = lessonType;
        this.teacher = teacher;
        this.group = group;
        this.coursePlan = coursePlan;
        this.subgroup = subgroup;
    }

    public com.sergofoox.domain.plan.Periodicity getPeriodicity() {
        return periodicity;
    }

    public void setPeriodicity(com.sergofoox.domain.plan.Periodicity periodicity) {
        this.periodicity = periodicity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public LessonType getLessonType() {
        return lessonType;
    }

    public void setLessonType(LessonType lessonType) {
        this.lessonType = lessonType;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public CoursePlan getCoursePlan() {
        return coursePlan;
    }

    public void setCoursePlan(CoursePlan coursePlan) {
        this.coursePlan = coursePlan;
    }

    public Timeslot getTimeslot() {
        return timeslot;
    }

    public void setTimeslot(Timeslot timeslot) {
        this.timeslot = timeslot;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Integer getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(Integer subgroup) {
        this.subgroup = subgroup;
    }

    public Integer getSplitGroupIndex() {
        return splitGroupIndex;
    }

    public void setSplitGroupIndex(Integer splitGroupIndex) {
        this.splitGroupIndex = splitGroupIndex;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    @Override
    public String toString() {
        return "Lesson(" + id + ")";
    }
}
