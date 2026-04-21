# План впровадження сутностей Subject та TeacherCompetenceMatrix

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Впровадження сутності `Subject` та сутності `TeacherCompetenceMatrix` для визначення компетенцій викладачів.

**Архітектура:** JPA-сутності за стандартними шаблонами POJO у пакетах `com.sergofoox.entity` та `com.sergofoox.domain.competence`.

**Технологічний стек:** Java 21, Spring Boot 4.x (Jakarta Persistence), JUnit 5.

---

### Завдання 1: Впровадження сутності `Subject` та перерахування `LessonType`

**Файли:**
- Створити: `src/main/java/com/sergofoox/entity/Subject.java`
- Створити: `src/main/java/com/sergofoox/entity/LessonType.java`
- Створити: `src/test/java/com/sergofoox/entity/SubjectTest.java`

- [ ] **Крок 1: Написання тесту для створення Subject, що не проходить**

```java
package com.sergofoox.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SubjectTest {
    @Test
    void testSubjectCreation() {
        Subject subject = new Subject("Mathematics", "Math");
        assertEquals("Mathematics", subject.getName());
        assertEquals("Math", subject.getAbbreviation());
    }
}
```

- [ ] **Крок 2: Запуск тесту для підтвердження невдачі**

Виконати: `./mvnw test -Dtest=SubjectTest`
Очікується: ПОМИЛКА (Помилка компіляції: клас Subject не існує)

- [ ] **Крок 3: Впровадження перерахування LessonType**

```java
package com.sergofoox.entity;

public enum LessonType {
    LECTURE,
    PRACTICE,
    LABORATORY
}
```

- [ ] **Крок 4: Впровадження сутності Subject**

```java
package com.sergofoox.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String abbreviation;

    public Subject() {}

    public Subject(String name, String abbreviation) {
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public Subject(Long id, String name, String abbreviation) {
        this.id = id;
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subject subject = (Subject) o;
        return Objects.equals(id, subject.id) &&
               Objects.equals(name, subject.name) &&
               Objects.equals(abbreviation, subject.abbreviation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, abbreviation);
    }

    @Override
    public String toString() {
        return "Subject{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", abbreviation='" + abbreviation + '\'' +
                '}';
    }
}
```

- [ ] **Крок 5: Запуск тесту для підтвердження успіху**

Виконати: `./mvnw test -Dtest=SubjectTest`
Очікується: УСПІШНО

- [ ] **Крок 6: Коміт**

```bash
git add src/main/java/com/sergofoox/entity/Subject.java src/main/java/com/sergofoox/entity/LessonType.java src/test/java/com/sergofoox/entity/SubjectTest.java
git commit -m "feat: add Subject entity and LessonType enum"
```

---

### Завдання 2: Впровадження перерахування `Priority` та сутності `TeacherCompetenceMatrix`

**Файли:**
- Створити: `src/main/java/com/sergofoox/domain/competence/Priority.java`
- Створити: `src/main/java/com/sergofoox/domain/competence/TeacherCompetenceMatrix.java`
- Створити: `src/test/java/com/sergofoox/domain/competence/TeacherCompetenceMatrixTest.java`

- [ ] **Крок 1: Написання тесту для створення TeacherCompetenceMatrix, що не проходить**

```java
package com.sergofoox.domain.competence;

import com.sergofoox.entity.Teacher;
import com.sergofoox.entity.Subject;
import com.sergofoox.entity.LessonType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeacherCompetenceMatrixTest {
    @Test
    void testMatrixCreation() {
        Teacher teacher = new Teacher("Ivanov Ivan", "CS", "Full");
        Subject subject = new Subject("Mathematics", "Math");
        TeacherCompetenceMatrix matrix = new TeacherCompetenceMatrix(teacher, subject, LessonType.LECTURE, Priority.HIGH);
        
        assertEquals(teacher, matrix.getTeacher());
        assertEquals(subject, matrix.getSubject());
        assertEquals(LessonType.LECTURE, matrix.getLessonType());
        assertEquals(Priority.HIGH, matrix.getPriority());
    }
}
```

- [ ] **Крок 2: Запуск тесту для підтвердження невдачі**

Виконати: `./mvnw test -Dtest=TeacherCompetenceMatrixTest`
Очікується: ПОМИЛКА (Помилка компіляції: класи Priority та TeacherCompetenceMatrix не існують)

- [ ] **Крок 3: Впровадження перерахування Priority**

```java
package com.sergofoox.domain.competence;

public enum Priority {
    HIGH,
    MEDIUM,
    LOW
}
```

- [ ] **Крок 4: Впровадження сутності TeacherCompetenceMatrix**

```java
package com.sergofoox.domain.competence;

import com.sergofoox.entity.Teacher;
import com.sergofoox.entity.Subject;
import com.sergofoox.entity.LessonType;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
public class TeacherCompetenceMatrix {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonType lessonType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    public TeacherCompetenceMatrix() {}

    public TeacherCompetenceMatrix(Teacher teacher, Subject subject, LessonType lessonType, Priority priority) {
        this.teacher = teacher;
        this.subject = subject;
        this.lessonType = lessonType;
        this.priority = priority;
    }

    public TeacherCompetenceMatrix(Long id, Teacher teacher, Subject subject, LessonType lessonType, Priority priority) {
        this.id = id;
        this.teacher = teacher;
        this.subject = subject;
        this.lessonType = lessonType;
        this.priority = priority;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public LessonType getLessonType() { return lessonType; }
    public void setLessonType(LessonType lessonType) { this.lessonType = lessonType; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeacherCompetenceMatrix that = (TeacherCompetenceMatrix) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(teacher, that.teacher) &&
               Objects.equals(subject, that.subject) &&
               lessonType == that.lessonType &&
               priority == that.priority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, teacher, subject, lessonType, priority);
    }

    @Override
    public String toString() {
        return "TeacherCompetenceMatrix{" +
                "id=" + id +
                ", teacher=" + teacher +
                ", subject=" + subject +
                ", lessonType=" + lessonType +
                ", priority=" + priority +
                '}';
    }
}
```

- [ ] **Крок 5: Запуск тесту для підтвердження успіху**

Виконати: `./mvnw test -Dtest=TeacherCompetenceMatrixTest`
Очікується: УСПІШНО

- [ ] **Крок 6: Коміт**

```bash
git add src/main/java/com/sergofoox/domain/competence/Priority.java src/main/java/com/sergofoox/domain/competence/TeacherCompetenceMatrix.java src/test/java/com/sergofoox/domain/competence/TeacherCompetenceMatrixTest.java
git commit -m "feat: add TeacherCompetenceMatrix entity and Priority enum"
```
