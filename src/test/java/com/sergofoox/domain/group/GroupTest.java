package com.sergofoox.domain.group;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroupTest {
    @Test
    void testGroupCreation() {
        Group group = new Group("KB-41", 25, 4, "Computer Science");
        assertEquals("KB-41", group.getName());
        assertEquals(25, group.getSize());
        assertEquals(4, group.getCourse());
        assertEquals("Computer Science", group.getDepartment());
    }

    @Test
    void testEquality() {
        Group g1 = new Group("KB-41", 25, 4, "CS");
        Group g2 = new Group("KB-41", 25, 4, "CS");
        assertEquals(g1, g2);
        assertEquals(g1.hashCode(), g2.hashCode());
    }
}
