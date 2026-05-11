package com.sergofoox.domain.autosave;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergofoox.domain.group.GroupRepository;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.plan.CoursePlanRepository;
import com.sergofoox.domain.room.RoomRepository;
import com.sergofoox.domain.subject.SubjectRepository;
import com.sergofoox.domain.teacher.TeacherRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutosaveService {

    private final AutosaveRepository autosaveRepository;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final RoomRepository roomRepository;
    private final SubjectRepository subjectRepository;
    private final CoursePlanRepository coursePlanRepository;
    private final LessonRepository lessonRepository;
    private final ObjectMapper objectMapper;

    public AutosaveService(AutosaveRepository autosaveRepository,
                           TeacherRepository teacherRepository,
                           GroupRepository groupRepository,
                           RoomRepository roomRepository,
                           SubjectRepository subjectRepository,
                           CoursePlanRepository coursePlanRepository,
                           LessonRepository lessonRepository,
                           ObjectMapper objectMapper) {
        this.autosaveRepository = autosaveRepository;
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.roomRepository = roomRepository;
        this.subjectRepository = subjectRepository;
        this.coursePlanRepository = coursePlanRepository;
        this.lessonRepository = lessonRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void captureSnapshot() {
        try {
            Map<String, Object> data = new HashMap<>();
            
            var teachers = teacherRepository.findAll();
            var groups = groupRepository.findAll();
            var rooms = roomRepository.findAll();
            var subjects = subjectRepository.findAll();
            var coursePlans = coursePlanRepository.findAll();
            var lessons = lessonRepository.findAll();

            data.put("teachers", teachers);
            data.put("groups", groups);
            data.put("rooms", rooms);
            data.put("subjects", subjects);
            data.put("coursePlans", coursePlans);
            data.put("lessons", lessons);

            String json = objectMapper.writeValueAsString(data);
            int totalCount = teachers.size() + groups.size() + rooms.size() + 
                             subjects.size() + coursePlans.size() + lessons.size();

            AutosaveSnapshot snapshot = new AutosaveSnapshot(LocalDateTime.now(), json, totalCount);
            autosaveRepository.save(snapshot);

            // Rotation: Keep only the 5 latest snapshots
            List<AutosaveSnapshot> snapshots = autosaveRepository.findAllByOrderByTimestampDesc();
            if (snapshots.size() > 5) {
                List<AutosaveSnapshot> toDelete = snapshots.subList(5, snapshots.size());
                autosaveRepository.deleteAll(toDelete);
            }
        } catch (JsonProcessingException e) {
            System.err.println(">>> Помилка серіалізації автозбереження: " + e.getMessage());
        } catch (Exception e) {
            System.err.println(">>> Непередбачена помилка при автозбереженні: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 120000, initialDelay = 60000) // Кожні 2 хвилини, запуск через 1 хв після старту
    public void autoSaveTask() {
        System.out.println(">>> Запуск фонового автозбереження...");
        captureSnapshot();
    }
}
