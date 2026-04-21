package com.sergofoox.domain.ui.dto;

import java.util.List;

public record ScheduleGridDTO(
    List<LessonDTO> lessons,
    List<TeacherDTO> teachers,
    List<GroupDTO> groups,
    List<RoomDTO> rooms,
    List<TimeslotDTO> timeslots
) {}
