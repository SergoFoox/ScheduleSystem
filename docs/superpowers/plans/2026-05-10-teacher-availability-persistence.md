# План впровадження збереження доступності викладачів

> **Для агентів:** НЕОБХІДНА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для виконання цього плану крок за кроком. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Створити рівень персистентності та API для керування доступністю викладачів.

**Архітектура:** Використання Spring Data JPA для репозиторію та Vaadin Hilla (BrowserCallable) для ендпоінту. Дані передаються через DTO record.

**Стек технологій:** Java 21, Spring Boot, Spring Data JPA, Vaadin Hilla.

---

### Завдання 1: Створення репозиторію доступності викладачів

**Файли:**
- Створити: `src/main/java/com/sergofoox/domain/teacher/TeacherAvailabilityRepository.java`

- [ ] **Крок 1: Створити інтерфейс репозиторію**

```java
package com.sergofoox.domain.teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TeacherAvailabilityRepository extends JpaRepository<TeacherAvailability, Long> {
    List<TeacherAvailability> findByTeacherId(Long teacherId);
    void deleteByTeacherId(Long teacherId);
}
```

- [ ] **Крок 2: Перевірити компіляцію**

Виконати: `./mvnw compile`
Очікується: BUILD SUCCESS

---

### Завдання 2: Створення DTO для доступності

**Файли:**
- Створити: `src/main/java/com/sergofoox/domain/ui/dto/AvailabilityDTO.java`

- [ ] **Крок 1: Створити record AvailabilityDTO**

```java
package com.sergofoox.domain.ui.dto;

import java.time.DayOfWeek;

public record AvailabilityDTO(DayOfWeek dayOfWeek, Integer lessonNumber, AvailabilityStatus status) {
}
```

- [ ] **Крок 2: Перевірити компіляцію**

Виконати: `./mvnw compile`
Очікується: BUILD SUCCESS

---

### Завдання 3: Створення Hilla ендпоінту

**Файли:**
- Створити: `src/main/java/com/sergofoox/domain/teacher/TeacherAvailabilityEndpoint.java`

- [ ] **Крок 1: Створити клас ендпоінту**

```java
package com.sergofoox.domain.teacher;

import com.sergofoox.domain.ui.TemplateAccessService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@BrowserCallable
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
```

- [ ] **Крок 2: Перевірити фінальну компіляцію**

Виконати: `./mvnw compile`
Очікується: BUILD SUCCESS
