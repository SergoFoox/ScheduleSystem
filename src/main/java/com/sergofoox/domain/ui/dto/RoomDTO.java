package com.sergofoox.domain.ui.dto;

import com.sergofoox.domain.plan.RoomType;

public record RoomDTO(
    Long id,
    String name,
    Integer capacity,
    String building,
    String equipment,
    RoomType type
) {}
