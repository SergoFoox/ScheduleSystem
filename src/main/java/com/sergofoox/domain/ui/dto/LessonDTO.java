package com.sergofoox.domain.ui.dto;

import com.sergofoox.domain.plan.Periodicity;

public record LessonDTO(
    Long id,
    String subjectName,
    String teacherName,
    String groupName,
    String roomName,
    Long timeslotId,
    boolean hasConflict,
    int subgroup,
    Long groupId,
    Long subjectId,
    Long roomId,
    Long teacherId,
    Periodicity periodicity
) {}
