# План впровадження сутностей Group та CoursePlan

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Впровадження сутності `Group` та сутності `CoursePlan` для визначення студентських груп та їхніх академічних вимог.

**Архітектура:** JPA-сутності за стандартними шаблонами POJO у пакетах `com.sergofoox.domain.group` та `com.sergofoox.domain.plan`.

**Технологічний стек:** Java 21, Spring Boot 4.x (Jakarta Persistence), Jakarta Validation, JUnit 5.

---

### Завдання 1: Впровадження сутності `Group`

**Файли:**
- Створити: `src/main/java/com/sergofoox/domain/group/Group.java`
- Створити: `src/test/java/com/sergofoox/domain/group/GroupTest.java`

- [x] **Крок 1: Написання тесту для створення Group та перевірки рівності, що не проходить**

```java
package com.sergofoox.domain.group;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroupTest {
    @Test
    void testGroupCreation() {
        Group group = new Group("KB-41", 25, 4, "Computer Science");
        assertEquals("KB-41", group.getName());
        assertEquals(25, group.getSize());
        assertEquals(4, group.getCourse());
        assertEquals("Computer Science", group.getDepartment());
    }

    @Test
    void testEquality() {
        Group g1 = new Group("KB-41", 25, 4, "CS");
        Group g2 = new Group("KB-41", 25, 4, "CS");
        assertEquals(g1, g2);
        assertEquals(g1.hashCode(), g2.hashCode());
    }
}
```

- [x] **Крок 2: Запуск тесту для підтвердження невдачі**

Виконати: `./mvnw test -Dtest=GroupTest`
Очікується: ПОМИЛКА (Помилка компіляції: клас Group не існує)

- [x] **Крок 3: Впровадження сутності Group**

```java
package com.sergofoox.domain.group;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Objects;

@Entity
@Table(name = "student_group") // 'group' є зарезервованим ключовим словом у SQL
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer size;

    @NotNull
    @Min(1)
    @Max(4)
    @Column(nullable = false)
    private Integer course;

    @NotBlank
    @Column(nullable = false)
    private String department;

    public Group() {}

    public Group(String name, Integer size, Integer course, String department) {
        this.name = name;
        this.size = size;
        this.course = course;
        this.department = department;
    }

    public Group(Long id, String name, Integer size, Integer course, String department) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.course = course;
        this.department = department;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public Integer getCourse() { return course; }
    public void setCourse(Integer course) { this.course = course; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group group)) return false;
        return Objects.equals(name, group.name) &&
               Objects.equals(course, group.course) &&
               Objects.equals(department, group.department);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, course, department);
    }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", course=" + course +
                ", department='" + department + '\'' +
                '}';
    }
}
```

- [x] **Крок 4: Запуск тесту для підтвердження успіху**

Виконати: `./mvnw test -Dtest=GroupTest`
Очікується: УСПІШНО

- [x] **Крок 5: Коміт**

```bash
git add src/main/java/com/sergofoox/domain/group/Group.java src/test/java/com/sergofoox/domain/group/GroupTest.java
git commit -m "feat: add Group entity"
```

---

### Завдання 2: Впровадження сутності `CoursePlan` та перерахування `RoomType`

**Файли:**
- Створити: `src/main/java/com/sergofoox/domain/plan/RoomType.java`
- Створити: `src/main/java/com/sergofoox/domain/plan/CoursePlan.java`
- Створити: `src/test/java/com/sergofoox/domain/plan/CoursePlanTest.java`

- [ ] **Крок 1: Написання тесту створення CoursePlan, що не проходить**

```java
package com.sergofoox.domain.plan;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.subject.Subject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoursePlanTest {
    @Test
    void testPlanCreation() {
        Subject subject = new Subject("Math", "M");
        Group group = new Group("KB-41", 25, 4, "CS");
        CoursePlan plan = new CoursePlan(subject, group, 120, 40, 40, 40, 1, 1, 1, RoomType.LECTURE_HALL);
        
        assertEquals(subject, plan.getSubject());
        assertEquals(group, plan.getGroup());
        assertEquals(120, plan.getTotalHours());
        assertEquals(RoomType.LECTURE_HALL, plan.getRequiredRoomType());
    }
}
```

- [x] **Крок 2: Запуск тесту для підтвердження невдачі**

Виконати: `./mvnw test -Dtest=CoursePlanTest`
Очікується: ПОМИЛКА (Помилка компіляції: класи RoomType та CoursePlan не існують)

- [ ] **Крок 3: Впровадження перерахування RoomType**

```java
package com.sergofoox.domain.plan;

public enum RoomType {
    LECTURE_HALL,
    LABORATORY,
    COMPUTER_CLASS,
    GENERAL_CLASSROOM
}
```

- [ ] **Крок 4: Впровадження сутності CoursePlan**

```java
package com.sergofoox.domain.plan;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.subject.Subject;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Objects;

@Entity
public class CoursePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Subject subject;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Group group;

    @NotNull
    @Min(0)
    private Integer totalHours;

    @NotNull
    @Min(0)
    private Integer lectureHours;

    @NotNull
    @Min(0)
    private Integer practiceHours;

    @NotNull
    @Min(0)
    private Integer labHours;

    @NotNull
    @Min(0)
    private Integer lectureSessionsPerWeek;

    @NotNull
    @Min(0)
    private Integer practiceSessionsPerWeek;

    @NotNull
    @Min(0)
    private Integer labSessionsPerWeek;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RoomType requiredRoomType;

    public CoursePlan() {}

    public CoursePlan(Subject subject, Group group, Integer totalHours, Integer lectureHours, Integer practiceHours, Integer labHours, Integer lectureSessionsPerWeek, Integer practiceSessionsPerWeek, Integer labSessionsPerWeek, RoomType requiredRoomType) {
        this.subject = subject;
        this.group = group;
        this.totalHours = totalHours;
        this.lectureHours = lectureHours;
        this.practiceHours = practiceHours;
        this.labHours = labHours;
        this.lectureSessionsPerWeek = lectureSessionsPerWeek;
        this.practiceSessionsPerWeek = practiceSessionsPerWeek;
        this.labSessionsPerWeek = labSessionsPerWeek;
        this.requiredRoomType = requiredRoomType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public Integer getTotalHours() { return totalHours; }
    public void setTotalHours(Integer totalHours) { this.totalHours = totalHours; }
    public Integer getLectureHours() { return lectureHours; }
    public void setLectureHours(Integer lectureHours) { this.lectureHours = lectureHours; }
    public Integer getPracticeHours() { return practiceHours; }
    public void setPracticeHours(Integer practiceHours) { this.practiceHours = practiceHours; }
    public Integer getLabHours() { return labHours; }
    public void setLabHours(Integer labHours) { this.labHours = labHours; }
    public Integer getLectureSessionsPerWeek() { return lectureSessionsPerWeek; }
    public void setLectureSessionsPerWeek(Integer lectureSessionsPerWeek) { this.lectureSessionsPerWeek = lectureSessionsPerWeek; }
    public Integer getPracticeSessionsPerWeek() { return practiceSessionsPerWeek; }
    public void setPracticeSessionsPerWeek(Integer practiceSessionsPerWeek) { this.practiceSessionsPerWeek = practiceSessionsPerWeek; }
    public Integer getLabSessionsPerWeek() { return labSessionsPerWeek; }
    public void setLabSessionsPerWeek(Integer labSessionsPerWeek) { this.labSessionsPerWeek = labSessionsPerWeek; }
    public RoomType getRequiredRoomType() { return requiredRoomType; }
    public void setRequiredRoomType(RoomType requiredRoomType) { this.requiredRoomType = requiredRoomType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CoursePlan that)) return false;
        return Objects.equals(subject, that.subject) &&
               Objects.equals(group, that.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, group);
    }
}
```

- [ ] **Крок 5: Запуск тесту для підтвердження успіху**

Виконати: `./mvnw test -Dtest=CoursePlanTest`
Очікується: УСПІШНО

- [ ] **Крок 6: Коміт**

```bash
git add src/main/java/com/sergofoox/domain/plan/RoomType.java src/main/java/com/sergofoox/domain/plan/CoursePlan.java src/test/java/com/sergofoox/domain/plan/CoursePlanTest.java
git commit -m "feat: add CoursePlan entity and RoomType enum"
```
