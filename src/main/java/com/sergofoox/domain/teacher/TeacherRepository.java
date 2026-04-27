package com.sergofoox.domain.teacher;

import com.sergofoox.domain.room.Room;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    @Override
    @EntityGraph(attributePaths = "assignedRoom")
    List<Teacher> findAll();

    List<Teacher> findByAssignedRoom(Room assignedRoom);
}
