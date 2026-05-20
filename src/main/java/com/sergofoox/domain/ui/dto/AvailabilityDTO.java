package com.sergofoox.domain.ui.dto;

import com.sergofoox.domain.teacher.AvailabilityStatus;
import java.time.DayOfWeek;

public record AvailabilityDTO(DayOfWeek dayOfWeek, Integer lessonNumber, AvailabilityStatus status) {
}
