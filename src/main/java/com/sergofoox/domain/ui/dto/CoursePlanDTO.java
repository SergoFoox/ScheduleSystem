package com.sergofoox.domain.ui.dto;

import com.sergofoox.domain.plan.RoomType;
import com.sergofoox.domain.plan.Periodicity;

public record CoursePlanDTO(
    Long id,
    Long groupId,
    Long subjectId,
    String subjectName,
    Long teacherId,
    String teacherName,
    Integer totalHours,
    Integer lectureHours,
    Integer practiceHours,
    Integer labHours,
    Integer lectureSessionsPerWeek,
    Integer practiceSessionsPerWeek,
    Integer labSessionsPerWeek,
    RoomType requiredRoomType,
    Periodicity lecturePeriodicity,
    Periodicity practicePeriodicity,
    Periodicity labPeriodicity
) {}
