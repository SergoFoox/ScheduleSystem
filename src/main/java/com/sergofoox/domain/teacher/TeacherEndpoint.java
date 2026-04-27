package com.sergofoox.domain.teacher;

import com.sergofoox.domain.ui.dto.TeacherDTO;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.room.RoomRepository;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@BrowserCallable
@Service
@AnonymousAllowed
public class TeacherEndpoint {

    private final TeacherRepository teacherRepository;
    private final LessonRepository lessonRepository;
    private final RoomRepository roomRepository;

    public TeacherEndpoint(TeacherRepository teacherRepository, LessonRepository lessonRepository, RoomRepository roomRepository) {
        this.teacherRepository = teacherRepository;
        this.lessonRepository = lessonRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public List<TeacherDTO> getAllTeachers() {
        return teacherRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public void saveTeacher(TeacherDTO dto) {
        System.out.println("Attempting to save teacher: " + dto.fullName());
        try {
            Teacher teacher;
            if (dto.id() != null) {
                teacher = teacherRepository.findById(dto.id()).orElseThrow();
            } else {
                teacher = new Teacher();
            }
            
            teacher.setFullName(dto.fullName());
            teacher.setDepartment(dto.department());
            teacher.setSpecialization(dto.specialization());
            teacher.setPositionType(dto.positionType());
            teacher.setWeeklyHourLimit(dto.weeklyHourLimit());
            teacher.setMaxWorkingDaysPerWeek(dto.maxWorkingDaysPerWeek());
            teacher.setAssignedRoom(dto.assignedRoomId() != null ? roomRepository.findById(dto.assignedRoomId()).orElse(null) : null);
            
            teacherRepository.save(teacher);
            System.out.println("Teacher saved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public void deleteTeacher(Long id) {
        try {
            Teacher teacher = teacherRepository.findById(id).orElseThrow();
            // Спершу видаляємо заняття, щоб не було конфліктів у БД
            lessonRepository.deleteByTeacher(teacher);
            teacherRepository.delete(teacher);
            System.out.println("Teacher deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private TeacherDTO mapToDTO(Teacher teacher) {
        return new TeacherDTO(
                teacher.getId(),
                teacher.getFullName(),
                teacher.getDepartment(),
                teacher.getSpecialization(),
                teacher.getPositionType(),
                teacher.getWeeklyHourLimit(),
                teacher.getMaxWorkingDaysPerWeek(),
                teacher.getAssignedRoom() != null ? teacher.getAssignedRoom().getId() : null,
                teacher.getAssignedRoom() != null ? teacher.getAssignedRoom().getName() : null
        );
    }
}
