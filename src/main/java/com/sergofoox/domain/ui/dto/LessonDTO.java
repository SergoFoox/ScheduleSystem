package com.sergofoox.domain.ui.dto;

public record LessonDTO(
    Long id,
    String subjectName,
    String teacherName,
    String groupName,
    String roomName,
    Long timeslotId,
    boolean hasConflict
) {}
