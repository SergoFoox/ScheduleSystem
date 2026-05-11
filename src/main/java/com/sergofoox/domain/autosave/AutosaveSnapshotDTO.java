package com.sergofoox.domain.autosave;

/**
 * DTO for representing a summary of an autosave snapshot.
 */
public record AutosaveSnapshotDTO(
    Long id,
    String timestamp,
    Integer entityCount,
    boolean isManual
) {}
