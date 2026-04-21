package com.sergofoox.domain.plan;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.teacher.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoursePlanRepository extends JpaRepository<CoursePlan, Long> {
    List<CoursePlan> findByGroup(Group group);
    
    @Query("SELECT DISTINCT cp FROM Lesson l JOIN l.coursePlan cp WHERE l.teacher = :teacher")
    List<CoursePlan> findByTeacher(Teacher teacher);
}
