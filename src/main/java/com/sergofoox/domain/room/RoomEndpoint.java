package com.sergofoox.domain.room;

import com.sergofoox.domain.ui.dto.RoomDTO;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.ui.TemplateAccessService;
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
    private final TeacherRepository teacherRepository;
    private final TemplateAccessService templateAccessService;

    public RoomEndpoint(RoomRepository roomRepository,
                        LessonRepository lessonRepository,
                        TeacherRepository teacherRepository,
                        TemplateAccessService templateAccessService) {
        this.roomRepository = roomRepository;
        this.lessonRepository = lessonRepository;
        this.teacherRepository = teacherRepository;
        this.templateAccessService = templateAccessService;
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
        templateAccessService.requireWritableTemplate();
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
        templateAccessService.requireWritableTemplate();
        try {
            Room room = roomRepository.findById(id).orElseThrow();
            
            // Instead of deleting lessons, simply unassign them from this room.
            List<Lesson> lessonsInRoom = lessonRepository.findByRoom(room);
            for (Lesson lesson : lessonsInRoom) {
                lesson.setRoom(null);
                lessonRepository.save(lesson);
            }

            List<Teacher> teachersInRoom = teacherRepository.findByAssignedRoom(room);
            for (Teacher teacher : teachersInRoom) {
                teacher.setAssignedRoom(null);
                teacherRepository.save(teacher);
            }
            
            roomRepository.delete(room);
            System.out.println("Room deleted successfully, lessons and teachers updated");
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
