package com.sergofoox.domain.lesson;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Override
    @EntityGraph(attributePaths = {"subject", "teacher", "group", "timeslot", "room"})
    List<Lesson> findAll();

    @EntityGraph(attributePaths = {"subject", "teacher", "group", "timeslot", "room"})
    List<Lesson> findByTimeslotId(Long timeslotId);
}
