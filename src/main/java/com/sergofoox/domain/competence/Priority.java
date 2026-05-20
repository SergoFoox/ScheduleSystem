package com.sergofoox.domain.competence;

/**
 * Teacher priority for a subject according to requirement 2.1.
 */
public enum Priority {
    PRIMARY,    // Main lecturer or owner
    SECONDARY,  // Can teach the subject, but is not primary
    SUBSTITUTE  // Replacement only
}
