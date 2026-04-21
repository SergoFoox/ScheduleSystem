package com.sergofoox.domain.ui.dto;

import java.util.List;

public record ScheduleAnalyticsDTO(
    String entityName,
    String entityType,
    List<CourseWorkloadDTO> courses,
    int totalWindows
) {}
