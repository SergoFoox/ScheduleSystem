package com.sergofoox.domain.plan;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.subject.Subject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoursePlanTest {
    @Test
    void testPlanCreation() {
        Subject subject = new Subject("Math", "M");
        Group group = new Group("KB-41", 25, 4, "CS");
        CoursePlan plan = new CoursePlan(subject, group, 120, 40, 40, 40, 1, 1, 1, RoomType.LECTURE_HALL);
        
        assertEquals(subject, plan.getSubject());
        assertEquals(group, plan.getGroup());
        assertEquals(120, plan.getTotalHours());
        assertEquals(RoomType.LECTURE_HALL, plan.getRequiredRoomType());
    }

    @Test
    void testEquality() {
        Subject s1 = new Subject("Math", "M");
        Group g1 = new Group("KB-41", 25, 4, "CS");
        CoursePlan p1 = new CoursePlan(s1, g1, 120, 40, 40, 40, 1, 1, 1, RoomType.LECTURE_HALL);
        CoursePlan p2 = new CoursePlan(s1, g1, 120, 40, 40, 40, 1, 1, 1, RoomType.LECTURE_HALL);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
