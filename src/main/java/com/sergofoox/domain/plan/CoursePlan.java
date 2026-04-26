package com.sergofoox.domain.plan;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.teacher.Teacher;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

@Entity
public class CoursePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    private Teacher secondTeacher;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Group group;

    @NotNull
    @Min(0)
    private Integer totalHours;

    @NotNull
    @Min(0)
    private Integer lectureHours;

    @NotNull
    @Min(0)
    private Integer practiceHours;

    @NotNull
    @Min(0)
    private Integer labHours;

    @NotNull
    @Min(0)
    private Integer lectureSessionsPerWeek;

    @NotNull
    @Min(0)
    private Integer practiceSessionsPerWeek;

    @NotNull
    @Min(0)
    private Integer labSessionsPerWeek;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Periodicity lecturePeriodicity = Periodicity.WEEKLY;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Periodicity practicePeriodicity = Periodicity.WEEKLY;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Periodicity labPeriodicity = Periodicity.WEEKLY;

    @NotNull
    @Min(0)
    private Integer executedHours = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RoomType requiredRoomType;

    @AssertTrue(message = "Total hours must equal the sum of lecture, practice, and lab hours")
    public boolean isHoursConsistent() {
        if (totalHours == null || lectureHours == null || practiceHours == null || labHours == null) {
            return true; // Let @NotNull handle nulls
        }
        return totalHours == (lectureHours + practiceHours + labHours);
    }

    /**
     * Відсоток виконання навчального плану (згідно з ТЗ 3.3)
     */
    public double getCompletionPercentage() {
        if (totalHours == null || totalHours == 0) return 0;
        return (executedHours * 100.0) / totalHours;
    }

    public CoursePlan() {}

    public CoursePlan(Subject subject, Teacher teacher, Group group, Integer totalHours, Integer lectureHours, Integer practiceHours, Integer labHours, Integer lectureSessionsPerWeek, Integer practiceSessionsPerWeek, Integer labSessionsPerWeek, RoomType requiredRoomType) {
        this.subject = subject;
        this.teacher = teacher;
        this.group = group;
        this.totalHours = totalHours;
        this.lectureHours = lectureHours;
        this.practiceHours = practiceHours;
        this.labHours = labHours;
        this.lectureSessionsPerWeek = lectureSessionsPerWeek;
        this.practiceSessionsPerWeek = practiceSessionsPerWeek;
        this.labSessionsPerWeek = labSessionsPerWeek;
        this.requiredRoomType = requiredRoomType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }
    public Teacher getSecondTeacher() { return secondTeacher; }
    public void setSecondTeacher(Teacher secondTeacher) { this.secondTeacher = secondTeacher; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public Integer getTotalHours() { return totalHours; }
    public void setTotalHours(Integer totalHours) { this.totalHours = totalHours; }
    public Integer getLectureHours() { return lectureHours; }
    public void setLectureHours(Integer lectureHours) { this.lectureHours = lectureHours; }
    public Integer getPracticeHours() { return practiceHours; }
    public void setPracticeHours(Integer practiceHours) { this.practiceHours = practiceHours; }
    public Integer getLabHours() { return labHours; }
    public void setLabHours(Integer labHours) { this.labHours = labHours; }
    public Integer getLectureSessionsPerWeek() { return lectureSessionsPerWeek; }
    public void setLectureSessionsPerWeek(Integer lectureSessionsPerWeek) { this.lectureSessionsPerWeek = lectureSessionsPerWeek; }
    public Integer getPracticeSessionsPerWeek() { return practiceSessionsPerWeek; }
    public void setPracticeSessionsPerWeek(Integer practiceSessionsPerWeek) { this.practiceSessionsPerWeek = practiceSessionsPerWeek; }
    public Integer getLabSessionsPerWeek() { return labSessionsPerWeek; }
    public void setLabSessionsPerWeek(Integer labSessionsPerWeek) { this.labSessionsPerWeek = labSessionsPerWeek; }

    public Periodicity getLecturePeriodicity() { return lecturePeriodicity; }
    public void setLecturePeriodicity(Periodicity lecturePeriodicity) { this.lecturePeriodicity = lecturePeriodicity; }
    public Periodicity getPracticePeriodicity() { return practicePeriodicity; }
    public void setPracticePeriodicity(Periodicity practicePeriodicity) { this.practicePeriodicity = practicePeriodicity; }
    public Periodicity getLabPeriodicity() { return labPeriodicity; }
    public void setLabPeriodicity(Periodicity labPeriodicity) { this.labPeriodicity = labPeriodicity; }
    public Integer getExecutedHours() { return executedHours; }
    public void setExecutedHours(Integer executedHours) { this.executedHours = executedHours; }

    public RoomType getRequiredRoomType() { return requiredRoomType; }
    public void setRequiredRoomType(RoomType requiredRoomType) { this.requiredRoomType = requiredRoomType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CoursePlan that)) return false;
        return Objects.equals(subject, that.subject) &&
               Objects.equals(group, that.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, group);
    }

    @Override
    public String toString() {
        return "CoursePlan{" +
                "id=" + id +
                ", subject=" + subject +
                ", group=" + group +
                ", totalHours=" + totalHours +
                ", requiredRoomType=" + requiredRoomType +
                '}';
    }
}
