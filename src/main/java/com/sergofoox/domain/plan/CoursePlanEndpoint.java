package com.sergofoox.domain.plan;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.group.GroupRepository;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.SubjectRepository;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.ui.TemplateAccessService;
import com.sergofoox.domain.ui.dto.CoursePlanDTO;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@BrowserCallable
@Service
@AnonymousAllowed
public class CoursePlanEndpoint {

    private final CoursePlanRepository coursePlanRepository;
    private final GroupRepository groupRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final LessonRepository lessonRepository;
    private final TemplateAccessService templateAccessService;

    public CoursePlanEndpoint(CoursePlanRepository coursePlanRepository,
                              GroupRepository groupRepository,
                              SubjectRepository subjectRepository,
                              TeacherRepository teacherRepository,
                              LessonRepository lessonRepository,
                              TemplateAccessService templateAccessService) {
        this.coursePlanRepository = coursePlanRepository;
        this.groupRepository = groupRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.lessonRepository = lessonRepository;
        this.templateAccessService = templateAccessService;
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
    public int copyPlansFromGroup(Long sourceGroupId, Long targetGroupId) {
        templateAccessService.requireWritableTemplate();
        if (sourceGroupId == null || targetGroupId == null) {
            throw new IllegalArgumentException("Source and target groups are required");
        }
        if (sourceGroupId.equals(targetGroupId)) {
            throw new IllegalArgumentException("Source and target groups must be different");
        }

        Group sourceGroup = groupRepository.findById(sourceGroupId).orElseThrow();
        Group targetGroup = groupRepository.findById(targetGroupId).orElseThrow();

        Set<Long> existingSubjectIds = new HashSet<>();
        for (CoursePlan existingPlan : coursePlanRepository.findByGroup(targetGroup)) {
            existingSubjectIds.add(existingPlan.getSubject().getId());
        }

        int copiedCount = 0;
        for (CoursePlan sourcePlan : coursePlanRepository.findByGroup(sourceGroup)) {
            Long subjectId = sourcePlan.getSubject().getId();
            if (existingSubjectIds.contains(subjectId)) {
                continue;
            }

            coursePlanRepository.save(copyPlanForGroup(sourcePlan, targetGroup));
            existingSubjectIds.add(subjectId);
            copiedCount++;
        }

        return copiedCount;
    }

    @Transactional
    public void savePlan(CoursePlanDTO dto) {
        templateAccessService.requireWritableTemplate();
        try {
            CoursePlan plan;
            if (dto.id() != null) {
                plan = coursePlanRepository.findById(dto.id()).orElseThrow();
            } else {
                plan = new CoursePlan();
            }

            plan.setGroup(groupRepository.findById(dto.groupId()).orElseThrow());
            plan.setSubject(subjectRepository.findById(dto.subjectId()).orElseThrow());
            
            if (dto.teacherId() == null) {
                throw new IllegalArgumentException("Teacher is required for course plan");
            }
            if (dto.teacherId() != null) {
                plan.setTeacher(teacherRepository.findById(dto.teacherId()).orElseThrow());
            }
            if (dto.secondTeacherId() != null) {
                plan.setSecondTeacher(teacherRepository.findById(dto.secondTeacherId()).orElseThrow());
            } else {
                plan.setSecondTeacher(null);
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
        templateAccessService.requireWritableTemplate();
        try {
            CoursePlan plan = coursePlanRepository.findById(id).orElseThrow();
            lessonRepository.deleteByCoursePlan(plan);
            coursePlanRepository.delete(plan);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private CoursePlan copyPlanForGroup(CoursePlan sourcePlan, Group targetGroup) {
        CoursePlan copy = new CoursePlan();
        copy.setGroup(targetGroup);
        copy.setSubject(sourcePlan.getSubject());
        copy.setTeacher(sourcePlan.getTeacher());
        copy.setSecondTeacher(sourcePlan.getSecondTeacher());
        copy.setTotalHours(sourcePlan.getTotalHours());
        copy.setLectureHours(sourcePlan.getLectureHours());
        copy.setPracticeHours(sourcePlan.getPracticeHours());
        copy.setLabHours(sourcePlan.getLabHours());
        copy.setLectureSessionsPerWeek(sourcePlan.getLectureSessionsPerWeek());
        copy.setPracticeSessionsPerWeek(sourcePlan.getPracticeSessionsPerWeek());
        copy.setLabSessionsPerWeek(sourcePlan.getLabSessionsPerWeek());
        copy.setLecturePeriodicity(sourcePlan.getLecturePeriodicity());
        copy.setPracticePeriodicity(sourcePlan.getPracticePeriodicity());
        copy.setLabPeriodicity(sourcePlan.getLabPeriodicity());
        copy.setRequiredRoomType(sourcePlan.getRequiredRoomType());
        copy.setExecutedHours(0);
        return copy;
    }

    private CoursePlanDTO mapToDTO(CoursePlan plan) {
        return new CoursePlanDTO(
                plan.getId(),
                plan.getGroup().getId(),
                plan.getSubject().getId(),
                plan.getSubject().getName(),
                plan.getTeacher() != null ? plan.getTeacher().getId() : null,
                plan.getTeacher() != null ? plan.getTeacher().getFullName() : null,
                plan.getSecondTeacher() != null ? plan.getSecondTeacher().getId() : null,
                plan.getSecondTeacher() != null ? plan.getSecondTeacher().getFullName() : null,
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
