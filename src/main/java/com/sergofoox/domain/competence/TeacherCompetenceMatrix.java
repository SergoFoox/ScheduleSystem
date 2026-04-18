package com.sergofoox.domain.competence;

import com.sergofoox.entity.Teacher;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.LessonType;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
public class TeacherCompetenceMatrix {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonType lessonType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    public TeacherCompetenceMatrix() {}

    public TeacherCompetenceMatrix(Teacher teacher, Subject subject, LessonType lessonType, Priority priority) {
        this.teacher = teacher;
        this.subject = subject;
        this.lessonType = lessonType;
        this.priority = priority;
    }

    public TeacherCompetenceMatrix(Long id, Teacher teacher, Subject subject, LessonType lessonType, Priority priority) {
        this.id = id;
        this.teacher = teacher;
        this.subject = subject;
        this.lessonType = lessonType;
        this.priority = priority;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public LessonType getLessonType() { return lessonType; }
    public void setLessonType(LessonType lessonType) { this.lessonType = lessonType; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeacherCompetenceMatrix that = (TeacherCompetenceMatrix) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(teacher, that.teacher) &&
               Objects.equals(subject, that.subject) &&
               lessonType == that.lessonType &&
               priority == that.priority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, teacher, subject, lessonType, priority);
    }

    @Override
    public String toString() {
        return "TeacherCompetenceMatrix{" +
                "id=" + id +
                ", teacher=" + teacher +
                ", subject=" + subject +
                ", lessonType=" + lessonType +
                ", priority=" + priority +
                '}';
    }
}
