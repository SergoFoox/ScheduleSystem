package com.sergofoox.domain.subject;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SubjectTest {
    @Test
    void testSubjectCreation() {
        Subject subject = new Subject("Mathematics", "Math");
        assertEquals("Mathematics", subject.getName());
        assertEquals("Math", subject.getAbbreviation());
    }

    @Test
    void testAllArgsConstructorAndGetters() {
        Subject subject = new Subject(1L, "Physics", "Phys");
        assertEquals(1L, subject.getId());
        assertEquals("Physics", subject.getName());
        assertEquals("Phys", subject.getAbbreviation());
    }

    @Test
    void testEqualsAndHashCode() {
        Subject s1 = new Subject(1L, "Math", "M");
        Subject s2 = new Subject(1L, "Math", "M");
        Subject s3 = new Subject(2L, "Physics", "P");
        Subject s4 = new Subject(null, "Math", "M");
        Subject s5 = new Subject(null, "Math", "M");

        // Reflexive
        assertEquals(s1, s1);
        
        // Symmetric
        assertEquals(s1, s2);
        assertEquals(s2, s1);
        
        // Consistent with hashCode
        assertEquals(s1.hashCode(), s2.hashCode());
        
        // Not equal
        assertNotEquals(s1, s3);
        assertNotEquals(s1, null);
        assertNotEquals(s1, new Object());

        // Null IDs equality
        assertEquals(s4, s5);
        assertEquals(s4.hashCode(), s5.hashCode());
    }

    @Test
    void testValidation() {
        jakarta.validation.ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
        jakarta.validation.Validator validator = factory.getValidator();

        Subject invalidSubject = new Subject("", "");
        var violations = validator.validate(invalidSubject);
        assertFalse(violations.isEmpty(), "Should have violations for empty strings");
        
        Subject validSubject = new Subject("Mathematics", "MATH");
        assertTrue(validator.validate(validSubject).isEmpty());
    }
}
