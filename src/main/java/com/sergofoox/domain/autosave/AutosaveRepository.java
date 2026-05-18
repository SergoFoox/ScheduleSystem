package com.sergofoox.domain.autosave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface AutosaveRepository extends JpaRepository<AutosaveSnapshot, Long> {
    Optional<AutosaveSnapshot> findFirstByOrderByTimestampDesc();
    List<AutosaveSnapshot> findAllByOrderByTimestampDesc();
    List<AutosaveSnapshot> findByScheduleIdOrderByTimestampDesc(Long scheduleId);
    
    @Modifying
    @Transactional
    void deleteByScheduleId(Long scheduleId);
}
