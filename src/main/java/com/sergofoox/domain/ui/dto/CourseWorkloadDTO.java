package com.sergofoox.domain.ui.dto;

public record CourseWorkloadDTO(
    String subjectName,
    int executedHours,
    int totalHours,
    double percentage
) {}
