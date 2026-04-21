# План переміщення та вдосконалення сутності Teacher

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Перемістити сутність `Teacher` до доменного пакету та вдосконалити логіку сутності (рівність, відкладена вибірка (lazy fetching), валідація) для `Teacher`, `Subject` та `TeacherCompetenceMatrix`.

**Архітектура:** Узгодження з проблемно-орієнтованим проектуванням (DDD) шляхом переміщення сутностей у доменні пакети. Стандартизація рівності JPA-сутностей за допомогою бізнес-ключів та `instanceof`. Покращення продуктивності за допомогою відкладеного завантаження (lazy loading).

**Технологічний стек:** Java 21, Spring Boot 4.0.5, Jakarta Persistence (JPA), JUnit 5, Jakarta Validation.

---

### Завдання 1: Переміщення Teacher.java

**Файли:**
- Перемістити: `src/main/java/com/sergofoox/entity/Teacher.java` до `src/main/java/com/sergofoox/domain/teacher/Teacher.java`
- Змінити: `src/main/java/com/sergofoox/domain/teacher/Teacher.java` (оновлення пакету)

- [ ] **Крок 1: Переміщення файлу**
Виконати: `mkdir -p src/main/java/com/sergofoox/domain/teacher && mv src/main/java/com/sergofoox/entity/Teacher.java src/main/java/com/sergofoox/domain/teacher/Teacher.java`

- [ ] **Крок 2: Оновлення оголошення пакету в Teacher.java**
```java
package com.sergofoox.domain.teacher;
```

- [ ] **Крок 3: Стандартизація equals() у Teacher.java**
Використовувати `instanceof` та тільки бізнес-ключі (`fullName`, `department`, `positionType`).
```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Teacher)) return false;
        Teacher teacher = (Teacher) o;
        return java.util.Objects.equals(fullName, teacher.fullName) &&
               java.util.Objects.equals(department, teacher.department) &&
               java.util.Objects.equals(positionType, teacher.positionType);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(fullName, department, positionType);
    }
```

- [ ] **Крок 4: Коміт**
```bash
git add src/main/java/com/sergofoox/domain/teacher/Teacher.java
git rm src/main/java/com/sergofoox/entity/Teacher.java
git commit -m "refactor: relocate Teacher entity to domain package and refine equality"
```

### Завдання 2: Оновлення TeacherCompetenceMatrix.java

**Файли:**
- Змінити: `src/main/java/com/sergofoox/domain/competence/TeacherCompetenceMatrix.java`

- [ ] **Крок 1: Оновлення імпортів та додавання @NotNull**
Оновити імпорт `Teacher`. Додати `jakarta.validation.constraints.NotNull`.

- [ ] **Крок 2: Застосування FetchType.LAZY та @NotNull до полів**
```java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @NotNull
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @NotNull
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private LessonType lessonType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private Priority priority;
```

- [ ] **Крок 3: Вдосконалення equals() та hashCode()**
Використовувати бізнес-ключі: `teacher`, `subject`, `lessonType`. Використовувати `instanceof`.
```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeacherCompetenceMatrix)) return false;
        TeacherCompetenceMatrix that = (TeacherCompetenceMatrix) o;
        return Objects.equals(teacher, that.teacher) &&
               Objects.equals(subject, that.subject) &&
               lessonType == that.lessonType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(teacher, subject, lessonType);
    }
```

- [ ] **Крок 4: Коміт**
```bash
git add src/main/java/com/sergofoox/domain/competence/TeacherCompetenceMatrix.java
git commit -m "refactor: update TeacherCompetenceMatrix with lazy loading, validation, and business key equality"
```

### Завдання 3: Стандартизація Subject.java

**Файли:**
- Змінити: `src/main/java/com/sergofoox/domain/subject/Subject.java`

- [ ] **Крок 1: Перевірка, чи використовує equals() instanceof** (Вже зроблено під час дослідження, але забезпечуємо узгодженість у плані)

- [ ] **Крок 2: Коміт (якщо були внесені зміни)**
```bash
git add src/main/java/com/sergofoox/domain/subject/Subject.java
git commit -m "style: ensure Subject equality standardization"
```

### Завдання 4: Оновлення всіх тестів

**Файли:**
- Змінити: `src/test/java/com/sergofoox/domain/teacher/TeacherTest.java`
- Змінити: `src/test/java/com/sergofoox/domain/subject/SubjectTest.java`
- Змінити: `src/test/java/com/sergofoox/domain/competence/TeacherCompetenceMatrixTest.java`

- [ ] **Крок 1: Оновлення TeacherTest.java**
Виправити пакет та імпорти. Додати тест для рівності без ID.
```java
package com.sergofoox.domain.teacher;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeacherTest {
    @Test
    void testEqualityWithoutId() {
        Teacher t1 = new Teacher(1L, "Ivanov Ivan", "CS", "Full");
        Teacher t2 = new Teacher(2L, "Ivanov Ivan", "CS", "Full");
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }
}
```

- [ ] **Крок 2: Оновлення TeacherCompetenceMatrixTest.java**
Виправити імпорти. Додати тест для рівності за бізнес-ключами.
```java
package com.sergofoox.domain.competence;

import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.LessonType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeacherCompetenceMatrixTest {
    @Test
    void testBusinessKeyEquality() {
        Teacher t = new Teacher("Ivan", "CS", "Full");
        Subject s = new Subject("Math", "M");
        TeacherCompetenceMatrix m1 = new TeacherCompetenceMatrix(1L, t, s, LessonType.LECTURE, Priority.HIGH);
        TeacherCompetenceMatrix m2 = new TeacherCompetenceMatrix(2L, t, s, LessonType.LECTURE, Priority.LOW);
        
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }
}
```

- [ ] **Крок 3: Запуск усіх тестів**
Виконати: `./mvnw test`

- [ ] **Крок 4: Коміт тестів**
```bash
git add src/test/java/com/sergofoox/domain/teacher/TeacherTest.java src/test/java/com/sergofoox/domain/competence/TeacherCompetenceMatrixTest.java src/test/java/com/sergofoox/domain/subject/SubjectTest.java
git commit -m "test: update tests for relocated entities and new equality logic"
```
