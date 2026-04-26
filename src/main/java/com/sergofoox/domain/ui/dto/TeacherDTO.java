package com.sergofoox.domain.ui.dto;

import com.sergofoox.domain.teacher.PositionType;

public record TeacherDTO(
    Long id,
    String fullName,
    String department,
    String specialization,
    PositionType positionType,
    Integer weeklyHourLimit,
    Integer maxWorkingDaysPerWeek,
    Long assignedRoomId,
    String assignedRoomName
) {}
