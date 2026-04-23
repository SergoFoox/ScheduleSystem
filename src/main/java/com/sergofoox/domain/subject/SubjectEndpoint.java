package com.sergofoox.domain.subject;

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

    public SubjectEndpoint(SubjectRepository subjectRepository,
                          com.sergofoox.domain.plan.CoursePlanRepository coursePlanRepository,
                          com.sergofoox.domain.lesson.LessonRepository lessonRepository,
                          com.sergofoox.domain.competence.TeacherCompetenceMatrixRepository matrixRepository) {
        this.subjectRepository = subjectRepository;
        this.coursePlanRepository = coursePlanRepository;
        this.lessonRepository = lessonRepository;
        this.matrixRepository = matrixRepository;
    }

    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional
    public Subject saveSubject(Subject subject) {
        return subjectRepository.save(subject);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteSubject(Long id) {
        Subject subject = subjectRepository.findById(id).orElseThrow();
        
        // 1. Видаляємо всі заняття з цим предметом
        lessonRepository.deleteBySubject(subject);
        
        // 2. Видаляємо всі компетенції викладачів
        matrixRepository.deleteBySubject(subject);
        
        // 3. Видаляємо плани навантаження
        coursePlanRepository.deleteBySubject(subject);
        
        // 4. Тепер можна видалити сам предмет
        subjectRepository.delete(subject);
    }
}
