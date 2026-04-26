package com.sergofoox.domain.plan;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.group.GroupRepository;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.SubjectRepository;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.ui.dto.CoursePlanDTO;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@BrowserCallable
@Service
@AnonymousAllowed
public class CoursePlanEndpoint {

    private final CoursePlanRepository coursePlanRepository;
    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final LessonRepository lessonRepository;

    public CoursePlanEndpoint(CoursePlanRepository coursePlanRepository,
                              GroupRepository groupRepository,
                              SubjectRepository subjectRepository,
                              TeacherRepository teacherRepository,
                              LessonRepository lessonRepository) {
        this.coursePlanRepository = coursePlanRepository;
        this.groupRepository = groupRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.lessonRepository = lessonRepository;
    }

    public List<CoursePlanDTO> getPlansByGroup(Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        return coursePlanRepository.findByGroup(group).stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<CoursePlanDTO> getAllPlans() {
        return coursePlanRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public void savePlan(CoursePlanDTO dto) {
        try {
            CoursePlan plan;
            if (dto.id() != null) {
                plan = coursePlanRepository.findById(dto.id()).orElseThrow();
            } else {
                plan = new CoursePlan();
            }

            plan.setGroup(groupRepository.findById(dto.groupId()).orElseThrow());
            plan.setSubject(subjectRepository.findById(dto.subjectId()).orElseThrow());
            
            if (dto.teacherId() != null) {
                plan.setTeacher(teacherRepository.findById(dto.teacherId()).orElseThrow());
            }

            plan.setTotalHours(dto.totalHours());
            plan.setLectureHours(dto.lectureHours());
            plan.setPracticeHours(dto.practiceHours());
            plan.setLabHours(dto.labHours());
            
            // Заповнюємо кількість занять на тиждень (мінімум 1, якщо години > 0)
            plan.setLectureSessionsPerWeek(dto.lectureSessionsPerWeek() != null ? dto.lectureSessionsPerWeek() : (dto.lectureHours() > 0 ? 1 : 0));
            plan.setPracticeSessionsPerWeek(dto.practiceSessionsPerWeek() != null ? dto.practiceSessionsPerWeek() : (dto.practiceHours() > 0 ? 1 : 0));
            plan.setLabSessionsPerWeek(dto.labSessionsPerWeek() != null ? dto.labSessionsPerWeek() : (dto.labHours() > 0 ? 1 : 0));
            
            plan.setRequiredRoomType(dto.requiredRoomType());
            plan.setLecturePeriodicity(dto.lecturePeriodicity() != null ? dto.lecturePeriodicity() : Periodicity.WEEKLY);
            plan.setPracticePeriodicity(dto.practicePeriodicity() != null ? dto.practicePeriodicity() : Periodicity.WEEKLY);
            plan.setLabPeriodicity(dto.labPeriodicity() != null ? dto.labPeriodicity() : Periodicity.WEEKLY);

            coursePlanRepository.save(plan);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public void deletePlan(Long id) {
        try {
            CoursePlan plan = coursePlanRepository.findById(id).orElseThrow();
            lessonRepository.deleteByCoursePlan(plan);
            coursePlanRepository.delete(plan);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private CoursePlanDTO mapToDTO(CoursePlan plan) {
        return new CoursePlanDTO(
                plan.getId(),
                plan.getGroup().getId(),
                plan.getSubject().getId(),
                plan.getSubject().getName(),
                plan.getTeacher() != null ? plan.getTeacher().getId() : null,
                plan.getTeacher() != null ? plan.getTeacher().getFullName() : null,
                plan.getTotalHours(),
                plan.getLectureHours(),
                plan.getPracticeHours(),
                plan.getLabHours(),
                plan.getLectureSessionsPerWeek(),
                plan.getPracticeSessionsPerWeek(),
                plan.getLabSessionsPerWeek(),
                plan.getRequiredRoomType(),
                plan.getLecturePeriodicity(),
                plan.getPracticePeriodicity(),
                plan.getLabPeriodicity()
        );
    }
}
