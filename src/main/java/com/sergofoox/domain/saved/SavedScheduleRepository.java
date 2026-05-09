package com.sergofoox.domain.saved;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedScheduleRepository extends JpaRepository<SavedSchedule, Long> {

    Optional<SavedSchedule> findByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "lessons")
    List<SavedSchedule> findAllByOrderBySortOrderAscUpdatedAtDescIdAsc();
}
