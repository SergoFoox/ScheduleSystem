package com.sergofoox.domain.ui.dto;

public record GroupDTO(
    Long id,
    String name,
    Integer size,
    Integer course,
    String department,
    Long curatorId,
    String curatorName
) {}
