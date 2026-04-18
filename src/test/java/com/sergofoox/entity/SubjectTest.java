package com.sergofoox.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SubjectTest {
    @Test
    void testSubjectCreation() {
        Subject subject = new Subject("Mathematics", "Math");
        assertEquals("Mathematics", subject.getName());
        assertEquals("Math", subject.getAbbreviation());
    }
}
