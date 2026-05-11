package com.sergofoox.domain.autosave;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class AutosaveSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String snapshotData;

    @Column(nullable = false)
    private Integer entityCount;

    @Column(nullable = false)
    private boolean isManual = false;

    private Long scheduleId;

    public AutosaveSnapshot() {}

    public AutosaveSnapshot(LocalDateTime timestamp, String snapshotData, Integer entityCount) {
        this(timestamp, snapshotData, entityCount, false, null);
    }

    public AutosaveSnapshot(LocalDateTime timestamp, String snapshotData, Integer entityCount, boolean isManual, Long scheduleId) {
        this.timestamp = timestamp;
        this.snapshotData = snapshotData;
        this.entityCount = entityCount;
        this.isManual = isManual;
        this.scheduleId = scheduleId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getSnapshotData() { return snapshotData; }
    public void setSnapshotData(String snapshotData) { this.snapshotData = snapshotData; }
    public Integer getEntityCount() { return entityCount; }
    public void setEntityCount(Integer entityCount) { this.entityCount = entityCount; }
    public boolean isManual() { return isManual; }
    public void setManual(boolean manual) { isManual = manual; }
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AutosaveSnapshot that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AutosaveSnapshot{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", entityCount=" + entityCount +
                '}';
    }
}
