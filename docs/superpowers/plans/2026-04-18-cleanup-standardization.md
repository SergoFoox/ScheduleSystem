# План впровадження очищення та стандартизації

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Рефакторинг доменних сутностей для кращої валідації, безпеки JPA та стандартизації.

**Архітектура:** Оновлення сутностей обмеженнями Jakarta Validation та зіставленням шаблонів (pattern matching) Java 16+ у методі `equals()`. Стандартизація бізнес-ключів для `equals` та `hashCode`.

**Технологічний стек:** Java 21, Spring Boot, Jakarta Validation, JPA.

---

### Завдання 1: Рефакторинг сутності Teacher

**Файли:**
- Змінити: `src/main/java/com/sergofoox/domain/teacher/Teacher.java`
- Тест: `src/test/java/com/sergofoox/domain/teacher/TeacherTest.java`

- [ ] **Крок 1: Оновлення Teacher.java обмеженнями та зіставленням шаблонів**

```java
package com.sergofoox.domain.teacher;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

@Entity
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Column(nullable = false)
    private String fullName;

    @NotBlank(message = "Department is required")
    @Column(nullable = false)
    private String department;

    @NotBlank(message = "Position type is required")
    @Column(nullable = false)
    private String positionType;

    public Teacher() {}

    public Teacher(String fullName, String department, String positionType) {
        this.fullName = fullName;
        this.department = department;
        this.positionType = positionType;
    }

    public Teacher(Long id, String fullName, String department, String positionType) {
        this.id = id;
        this.fullName = fullName;
        this.department = department;
        this.positionType = positionType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getPositionType() { return positionType; }
    public void setPositionType(String positionType) { this.positionType = positionType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Teacher other)) return false;
        return Objects.equals(fullName, other.fullName) &&
               Objects.equals(department, other.department) &&
               Objects.equals(positionType, other.positionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName, department, positionType);
    }

    @Override
    public String toString() {
        return "Teacher{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", department='" + department + '\'' +
                ", positionType='" + positionType + '\'' +
                '}';
    }
}
```

- [ ] **Крок 2: Запуск тестів для перевірки змін у Teacher**

Виконати: `./mvnw test -Dtest=TeacherTest`
Очікується: УСПІШНО

### Завдання 2: Стандартизація сутності Room

**Файли:**
- Змінити: `src/main/java/com/sergofoox/domain/room/Room.java`
- Тест: `src/test/java/com/sergofoox/domain/room/RoomTest.java`

- [ ] **Крок 1: Оновлення Room.java зі зіставленням шаблонів у equals**

```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room other)) return false;
        return Objects.equals(name, other.name) && Objects.equals(building, other.building);
    }
```

- [ ] **Крок 2: Запуск тестів для перевірки змін у Room**

Виконати: `./mvnw test -Dtest=RoomTest`
Очікується: УСПІШНО

### Завдання 3: Стандартизація сутності TeacherCompetenceMatrix

**Файли:**
- Змінити: `src/main/java/com/sergofoox/domain/competence/TeacherCompetenceMatrix.java`
- Тест: `src/test/java/com/sergofoox/domain/competence/TeacherCompetenceMatrixTest.java`

- [ ] **Крок 1: Оновлення TeacherCompetenceMatrix.java зі зіставленням шаблонів у equals**

```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeacherCompetenceMatrix other)) return false;
        return Objects.equals(teacher, other.teacher) &&
               Objects.equals(subject, other.subject) &&
               lessonType == other.lessonType;
    }
```

- [ ] **Крок 2: Запуск тестів для перевірки змін у Matrix**

Виконати: `./mvnw test -Dtest=TeacherCompetenceMatrixTest`
Очікується: УСПІШНО

### Завдання 4: Стандартизація інших сутностей (Перевірка)

**Файли:**
- Змінити: `src/main/java/com/sergofoox/domain/subject/Subject.java`
- Змінити: `src/main/java/com/sergofoox/domain/group/Group.java`
- Змінити: `src/main/java/com/sergofoox/domain/plan/CoursePlan.java`

- [ ] **Крок 1: Перевірка та забезпечення зіставлення шаблонів у Subject.java** (Здається, вже правильно, але перевірте двічі)
- [ ] **Крок 2: Перевірка та забезпечення зіставлення шаблонів у Group.java** (Здається, вже правильно, але перевірте двічі)
- [ ] **Крок 3: Перевірка та забезпечення зіставлення шаблонів у CoursePlan.java** (Здається, вже правильно, але перевірте двічі)

### Завдання 5: Оновлення Memory Bank

**Файли:**
- Змінити: `memory-bank/progress.md`
- Змінити: `memory-bank/activeContext.md`

- [ ] **Крок 1: Оновлення progress.md**
  - Позначити `Subject`, `Group`, `CoursePlan`, `Room` та `Matrix` як завершені.
  - Перемістити "Define Domain Model" до завершених (Completed).
  - Додати "Timefold Integration" до завдань у процесі (In Progress).
- [ ] **Крок 2: Оновлення activeContext.md**
  - Оновити фокус на "Timefold Integration and Solver configuration".
  - Оновити наступні кроки на "Set up Timefold Solver and define initial constraints".

### Завдання 6: Фінальна перевірка та коміт

- [ ] **Крок 1: Запуск усіх доменних тестів**

Виконати: `./mvnw test`
Очікується: УСПІШНО

- [ ] **Крок 2: Фіксація змін (коміт)**

```bash
git add .
git commit -m "refactor: cleanup and standardize domain entities

- Add @NotBlank to Teacher fields
- Use Java 16+ pattern matching for equals() in all entities
- Ensure stable business keys for equals/hashCode
- Update Memory Bank to reflect completed domain model"
```
