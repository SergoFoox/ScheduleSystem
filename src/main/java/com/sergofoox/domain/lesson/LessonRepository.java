package com.sergofoox.domain.lesson;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Override
    @EntityGraph(attributePaths = {"subject", "teacher", "group", "timeslot", "room", "coursePlan"})
    List<Lesson> findAll();

    @EntityGraph(attributePaths = {"subject", "teacher", "group", "timeslot", "room", "coursePlan"})
    List<Lesson> findByTimeslotId(Long timeslotId);

    void deleteByTeacher(com.sergofoox.domain.teacher.Teacher teacher);
    void deleteByGroup(com.sergofoox.domain.group.Group group);
    void deleteByCoursePlan(com.sergofoox.domain.plan.CoursePlan coursePlan);
    void deleteBySubject(com.sergofoox.domain.subject.Subject subject);
    List<Lesson> findByRoom(com.sergofoox.domain.room.Room room);
}
