package com.sergofoox.domain.ui.dto;

public record SavedScheduleDTO(
        Long id,
        String name,
        String createdAt,
        String updatedAt,
        int lessonCount,
        boolean builtIn,
        boolean fullTemplate
) {
}
