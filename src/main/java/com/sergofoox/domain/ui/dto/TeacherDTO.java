package com.sergofoox.domain.ui.dto;

import com.sergofoox.domain.teacher.PositionType;

public record TeacherDTO(
    Long id,
    String fullName,
    String department,
    PositionType positionType
) {}
