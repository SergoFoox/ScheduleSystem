package com.sergofoox.domain.autosave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AutosaveRepository extends JpaRepository<AutosaveSnapshot, Long> {
    List<AutosaveSnapshot> findAllByOrderByTimestampDesc();
    List<AutosaveSnapshot> findByScheduleIdOrderByTimestampDesc(Long scheduleId);
}
