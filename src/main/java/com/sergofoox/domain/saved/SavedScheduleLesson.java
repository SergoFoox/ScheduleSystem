package com.sergofoox.domain.saved;

import com.sergofoox.domain.plan.Periodicity;
import com.sergofoox.domain.subject.LessonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class SavedScheduleLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private SavedSchedule savedSchedule;

    @Column(nullable = false)
    private Long lessonId;

    private Long coursePlanId;
    private Long groupId;
    private Long subjectId;
    private Long teacherId;
    private Long timeslotId;
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonType lessonType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Periodicity periodicity;

    private Integer subgroup;
    private Integer splitGroupIndex;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SavedSchedule getSavedSchedule() {
        return savedSchedule;
    }

    public void setSavedSchedule(SavedSchedule savedSchedule) {
        this.savedSchedule = savedSchedule;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public Long getCoursePlanId() {
        return coursePlanId;
    }

    public void setCoursePlanId(Long coursePlanId) {
        this.coursePlanId = coursePlanId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public Long getTimeslotId() {
        return timeslotId;
    }

    public void setTimeslotId(Long timeslotId) {
        this.timeslotId = timeslotId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public LessonType getLessonType() {
        return lessonType;
    }

    public void setLessonType(LessonType lessonType) {
        this.lessonType = lessonType;
    }

    public Periodicity getPeriodicity() {
        return periodicity;
    }

    public void setPeriodicity(Periodicity periodicity) {
        this.periodicity = periodicity;
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
}
