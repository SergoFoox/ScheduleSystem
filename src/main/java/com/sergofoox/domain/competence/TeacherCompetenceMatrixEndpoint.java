package com.sergofoox.domain.competence;

import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.SubjectRepository;
import com.sergofoox.domain.ui.TemplateAccessService;
import com.sergofoox.domain.ui.dto.TeacherCompetenceDTO;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@BrowserCallable
@Service
@AnonymousAllowed
public class TeacherCompetenceMatrixEndpoint {

    private final TeacherCompetenceMatrixRepository repository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final TemplateAccessService templateAccessService;

    public TeacherCompetenceMatrixEndpoint(TeacherCompetenceMatrixRepository repository,
                                          TeacherRepository teacherRepository,
                                          SubjectRepository subjectRepository,
                                          TemplateAccessService templateAccessService) {
        this.repository = repository;
        this.teacherRepository = teacherRepository;
        this.subjectRepository = subjectRepository;
        this.templateAccessService = templateAccessService;
    }

    public List<TeacherCompetenceDTO> getCompetencesByTeacher(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId).orElseThrow();
        return repository.findByTeacher(teacher).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public void saveCompetence(TeacherCompetenceDTO dto) {
        templateAccessService.requireWritableTemplate();
        TeacherCompetenceMatrix matrix;
        if (dto.id() != null) {
            matrix = repository.findById(dto.id()).orElseThrow();
        } else {
            matrix = new TeacherCompetenceMatrix();
        }

        matrix.setTeacher(teacherRepository.findById(dto.teacherId()).orElseThrow());
        matrix.setSubject(subjectRepository.findById(dto.subjectId()).orElseThrow());
        matrix.setLessonType(dto.lessonType());
        matrix.setPriority(dto.priority());

        repository.save(matrix);
    }

    @Transactional
    public void deleteCompetence(Long id) {
        templateAccessService.requireWritableTemplate();
        repository.deleteById(id);
    }

    private TeacherCompetenceDTO mapToDTO(TeacherCompetenceMatrix matrix) {
        return new TeacherCompetenceDTO(
                matrix.getId(),
                matrix.getTeacher().getId(),
                matrix.getSubject().getId(),
                matrix.getSubject().getName(),
                matrix.getLessonType(),
                matrix.getPriority()
        );
    }
}
