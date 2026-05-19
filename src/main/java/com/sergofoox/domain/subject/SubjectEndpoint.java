package com.sergofoox.domain.subject;

import com.sergofoox.domain.ui.TemplateAccessService;
import com.sergofoox.domain.ui.dto.TeacherDTO;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.stereotype.Service;

import java.util.List;

@BrowserCallable
@Service
@AnonymousAllowed
public class SubjectEndpoint {

    private final SubjectRepository subjectRepository;
    private final com.sergofoox.domain.plan.CoursePlanRepository coursePlanRepository;
    private final com.sergofoox.domain.lesson.LessonRepository lessonRepository;
    private final com.sergofoox.domain.competence.TeacherCompetenceMatrixRepository matrixRepository;
    private final TemplateAccessService templateAccessService;

    public SubjectEndpoint(SubjectRepository subjectRepository,
                          com.sergofoox.domain.plan.CoursePlanRepository coursePlanRepository,
                          com.sergofoox.domain.lesson.LessonRepository lessonRepository,
                          com.sergofoox.domain.competence.TeacherCompetenceMatrixRepository matrixRepository,
                          TemplateAccessService templateAccessService) {
        this.subjectRepository = subjectRepository;
        this.coursePlanRepository = coursePlanRepository;
        this.lessonRepository = lessonRepository;
        this.matrixRepository = matrixRepository;
        this.templateAccessService = templateAccessService;
    }

    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional
    public Subject saveSubject(Subject subject) {
        templateAccessService.requireWritableTemplate();
        return subjectRepository.save(subject);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteSubject(Long id) {
        templateAccessService.requireWritableTemplate();
        Subject subject = subjectRepository.findById(id).orElseThrow();
        
        // 1. Delete all lessons for this subject.
        lessonRepository.deleteBySubject(subject);
        
        // 2. Delete all teacher competence entries.
        matrixRepository.deleteBySubject(subject);
        
        // 3. Delete all workload plans.
        coursePlanRepository.deleteBySubject(subject);
        
        // 4. The subject itself can now be deleted.
        subjectRepository.delete(subject);
    }
}
