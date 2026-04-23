package com.sergofoox.domain.room;

import com.sergofoox.domain.ui.dto.RoomDTO;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.lesson.LessonRepository;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@BrowserCallable
@Service
@AnonymousAllowed
public class RoomEndpoint {

    private final RoomRepository roomRepository;
    private final LessonRepository lessonRepository;

    public RoomEndpoint(RoomRepository roomRepository, LessonRepository lessonRepository) {
        this.roomRepository = roomRepository;
        this.lessonRepository = lessonRepository;
    }

    public List<RoomDTO> getAllRooms() {
        try {
            return roomRepository.findAll().stream()
                    .map(this::mapToDTO)
                    .toList();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public void saveRoom(RoomDTO dto) {
        System.out.println("Attempting to save room: " + dto.name());
        try {
            Room room;
            if (dto.id() != null) {
                room = roomRepository.findById(dto.id()).orElseThrow();
            } else {
                room = new Room();
            }
            
            room.setName(dto.name());
            room.setCapacity(dto.capacity());
            room.setBuilding(dto.building());
            room.setEquipment(dto.equipment());
            room.setType(dto.type());
            
            roomRepository.save(room);
            System.out.println("Room saved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public void deleteRoom(Long id) {
        try {
            Room room = roomRepository.findById(id).orElseThrow();
            
            // Замість видалення занять, ми просто "виписуємо" їх з цієї аудиторії
            List<Lesson> lessonsInRoom = lessonRepository.findByRoom(room);
            for (Lesson lesson : lessonsInRoom) {
                lesson.setRoom(null);
                lessonRepository.save(lesson);
            }
            
            roomRepository.delete(room);
            System.out.println("Room deleted successfully, lessons updated");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private RoomDTO mapToDTO(Room room) {
        return new RoomDTO(
                room.getId(),
                room.getName(),
                room.getCapacity(),
                room.getBuilding(),
                room.getEquipment(),
                room.getType()
        );
    }
}
