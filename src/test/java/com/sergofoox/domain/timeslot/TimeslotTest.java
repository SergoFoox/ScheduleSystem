package com.sergofoox.domain.timeslot;

import com.sergofoox.domain.plan.Periodicity;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class TimeslotTest {

    @Test
    void testTimeslotCreation() {
        LocalTime start = LocalTime.of(8, 30);
        LocalTime end = LocalTime.of(10, 0);
        Timeslot timeslot = new Timeslot(DayOfWeek.MONDAY, start, end, Periodicity.ODD_WEEKS, 1);

        assertEquals(DayOfWeek.MONDAY, timeslot.getDayOfWeek());
        assertEquals(start, timeslot.getStartTime());
        assertEquals(end, timeslot.getEndTime());
        assertEquals(Periodicity.ODD_WEEKS, timeslot.getWeekParity());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalTime start = LocalTime.of(8, 30);
        LocalTime end = LocalTime.of(10, 0);
        
        Timeslot t1 = new Timeslot(DayOfWeek.MONDAY, start, end, Periodicity.WEEKLY, 1);
        Timeslot t2 = new Timeslot(DayOfWeek.MONDAY, start, end, Periodicity.WEEKLY, 1);
        Timeslot t3 = new Timeslot(DayOfWeek.TUESDAY, start, end, Periodicity.WEEKLY, 1);

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertNotEquals(t1, t3);
    }

    @Test
    void testToString() {
        Timeslot timeslot = new Timeslot(DayOfWeek.WEDNESDAY, LocalTime.of(10, 15), LocalTime.of(11, 45), Periodicity.EVEN_WEEKS, 2);
        String expected = "WEDNESDAY 10:15-11:45 (EVEN_WEEKS)";
        assertEquals(expected, timeslot.toString());
    }
}
