package com.sergofoox.domain.room;

import com.sergofoox.domain.plan.RoomType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RoomTest {

    @Test
    void testRoomInstantiation() {
        Room room = new Room();
        room.setId(1L);
        room.setName("101");
        room.setBuilding("Building A");
        room.setCapacity(30);
        room.setType(RoomType.LECTURE_HALL);
        room.setEquipment("Projector");

        assertEquals(1L, room.getId());
        assertEquals("101", room.getName());
        assertEquals("Building A", room.getBuilding());
        assertEquals(30, room.getCapacity());
        assertEquals(RoomType.LECTURE_HALL, room.getType());
        assertEquals("Projector", room.getEquipment());
    }

    @Test
    void testEqualityByBusinessKey() {
        Room room1 = new Room();
        room1.setName("101");
        room1.setBuilding("Building A");
        room1.setCapacity(30);

        Room room2 = new Room();
        room2.setName("101");
        room2.setBuilding("Building A");
        room2.setCapacity(50); // Different capacity, but same business key

        assertEquals(room1, room2);
        assertEquals(room1.hashCode(), room2.hashCode());

        Room room3 = new Room();
        room3.setName("102");
        room3.setBuilding("Building A");

        assertNotEquals(room1, room3);
    }

    @Test
    void testConstructors() {
        Room room5 = new Room("101", 30, "Building A", "Projector", RoomType.LECTURE_HALL);
        assertEquals("101", room5.getName());
        assertEquals(30, room5.getCapacity());
        assertEquals("Building A", room5.getBuilding());
        assertEquals("Projector", room5.getEquipment());
        assertEquals(RoomType.LECTURE_HALL, room5.getType());

        Room room6 = new Room(1L, "102", 20, "Building B", "Computer", RoomType.LABORATORY);
        assertEquals(1L, room6.getId());
        assertEquals("102", room6.getName());
        assertEquals(20, room6.getCapacity());
        assertEquals("Building B", room6.getBuilding());
        assertEquals("Computer", room6.getEquipment());
        assertEquals(RoomType.LABORATORY, room6.getType());
    }
}
