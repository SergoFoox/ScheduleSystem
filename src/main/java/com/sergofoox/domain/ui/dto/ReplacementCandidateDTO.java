package com.sergofoox.domain.ui.dto;

import com.sergofoox.domain.competence.Priority;

public record ReplacementCandidateDTO(
    Long id,
    String fullName,
    String department,
    Priority priority,
    long currentWorkload
) {}
