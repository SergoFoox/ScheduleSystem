package com.sergofoox.domain.competence;

import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.LessonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherCompetenceMatrixRepository extends JpaRepository<TeacherCompetenceMatrix, Long> {
    List<TeacherCompetenceMatrix> findBySubjectAndLessonType(Subject subject, LessonType lessonType);
}
