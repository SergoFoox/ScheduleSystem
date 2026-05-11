package com.sergofoox.domain.ui.dto;

public record SavedScheduleDTO(
    Long id,
    String name,
    String createdAt,
    String updatedAt,
    Integer lessonCount,
    boolean isBuiltIn,
    boolean isFullTemplate,
    boolean autosaveEnabled
) {}
