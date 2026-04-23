package com.sergofoox.domain.ui.dto;

import com.sergofoox.domain.plan.Periodicity;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record TimeslotDTO(
    Long id,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    Periodicity weekParity,
    Integer lessonNumber
) {}
