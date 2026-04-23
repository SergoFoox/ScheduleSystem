package com.sergofoox.domain.ui.dto;

import com.sergofoox.domain.subject.LessonType;
import com.sergofoox.domain.competence.Priority;

public record TeacherCompetenceDTO(
    Long id,
    Long teacherId,
    Long subjectId,
    String subjectName,
    LessonType lessonType,
    Priority priority
) {}
