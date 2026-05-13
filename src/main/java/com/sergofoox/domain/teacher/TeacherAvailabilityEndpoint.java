package com.sergofoox.domain.teacher;

import com.sergofoox.domain.ui.TemplateAccessService;
import com.sergofoox.domain.ui.dto.AvailabilityDTO;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@BrowserCallable
@Service
@AnonymousAllowed
public class TeacherAvailabilityEndpoint {
    private final TeacherAvailabilityRepository repository;
    private final TeacherRepository teacherRepository;
    private final TemplateAccessService templateAccessService;

    public TeacherAvailabilityEndpoint(TeacherAvailabilityRepository repository,
                                       TeacherRepository teacherRepository,
                                       TemplateAccessService templateAccessService) {
        this.repository = repository;
        this.teacherRepository = teacherRepository;
        this.templateAccessService = templateAccessService;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityDTO> getAvailability(Long teacherId) {
        return repository.findByTeacherId(teacherId).stream()
                .map(a -> new AvailabilityDTO(a.getDayOfWeek(), a.getLessonNumber(), a.getStatus()))
                .toList();
    }

    @Transactional
    public void saveAvailability(Long teacherId, List<AvailabilityDTO> dtos) {
        templateAccessService.requireWritableTemplate();
        repository.deleteByTeacherId(teacherId);
        Teacher teacher = teacherRepository.findById(teacherId).orElseThrow();
        List<TeacherAvailability> entities = dtos.stream().map(dto -> {
            TeacherAvailability a = new TeacherAvailability();
            a.setTeacher(teacher);
            a.setDayOfWeek(dto.dayOfWeek());
            a.setLessonNumber(dto.lessonNumber());
            a.setStatus(dto.status());
            return a;
        }).toList();
        repository.saveAll(entities);
    }
}
