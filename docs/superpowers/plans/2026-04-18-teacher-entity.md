# План впровадження сутності Teacher

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Впровадження доменної сутності `Teacher` як керованого JPA об'єкта стійкості.

**Архітектура:** JPA-сутність за стандартними шаблонами POJO у пакеті `com.sergofoox.domain.teacher`.

**Технологічний стек:** Java 21, Spring Boot 4.x (Jakarta Persistence), JUnit 5.

---

### Завдання 1: Створення сутності Teacher та базового тесту

**Файли:**
- Створити: `src/main/java/com/sergofoox/domain/teacher/Teacher.java`
- Створити: `src/test/java/com/sergofoox/domain/teacher/TeacherTest.java`

- [ ] **Крок 1: Написання тесту створення Teacher, що не проходить**

```java
package com.sergofoox.domain.teacher;

import com.sergofoox.entity.Teacher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TeacherTest {
    @Test
    void testTeacherCreation() {
        Teacher teacher = new Teacher("Ivanov Ivan", "Computer Science", "Full-time");
        assertEquals("Ivanov Ivan", teacher.getFullName());
        assertEquals("Computer Science", teacher.getDepartment());
        assertEquals("Full-time", teacher.getPositionType());
    }
}
```

- [ ] **Крок 2: Запуск тесту для підтвердження невдачі**

Виконати: `./mvnw test -Dtest=TeacherTest`
Очікується: ПОМИЛКА (Помилка компіляції: клас Teacher не існує)

- [ ] **Крок 3: Написання мінімальної реалізації класу Teacher**

```java
package com.sergofoox.domain.teacher;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String positionType;

    public Teacher() {}

    public Teacher(String fullName, String department, String positionType) {
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
}
```

- [ ] **Крок 4: Запуск тесту для підтвердження успіху**

Виконати: `./mvnw test -Dtest=TeacherTest`
Очікується: УСПІШНО

- [ ] **Крок 5: Коміт**

```bash
git add src/main/java/com/sergofoox/domain/teacher/Teacher.java src/test/java/com/sergofoox/domain/teacher/TeacherTest.java
git commit -m "feat: add Teacher entity with basic tests"
```

---

### Завдання 2: Впровадження Equals, HashCode та ToString

**Файли:**
- Змінити: `src/main/java/com/sergofoox/domain/teacher/Teacher.java`
- Змінити: `src/test/java/com/sergofoox/domain/teacher/TeacherTest.java`

- [ ] **Крок 1: Написання тестів для рівності та toString, що не проходять**

```java
    @Test
    void testEquality() {
        Teacher t1 = new Teacher("Ivanov Ivan", "CS", "Full");
        Teacher t2 = new Teacher("Ivanov Ivan", "CS", "Full");
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void testToString() {
        Teacher teacher = new Teacher("Ivanov Ivan", "CS", "Full");
        assertTrue(teacher.toString().contains("Ivanov Ivan"));
    }
```

- [ ] **Крок 2: Запуск тестів для підтвердження невдачі**

Виконати: `./mvnw test -Dtest=TeacherTest`
Очікується: НЕВДАЧА (Методи Object за замовчуванням не відповідають бізнес-рівності)

- [ ] **Крок 3: Впровадження equals, hashCode та toString**

```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Teacher teacher = (Teacher) o;
        return java.util.Objects.equals(id, teacher.id) &&
               java.util.Objects.equals(fullName, teacher.fullName) &&
               java.util.Objects.equals(department, teacher.department) &&
               java.util.Objects.equals(positionType, teacher.positionType);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, fullName, department, positionType);
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
```

- [ ] **Крок 4: Запуск тестів для підтвердження успіху**

Виконати: `./mvnw test -Dtest=TeacherTest`
Очікується: УСПІШНО

- [ ] **Крок 5: Коміт**

```bash
git add src/main/java/com/sergofoox/domain/teacher/Teacher.java src/test/java/com/sergofoox/domain/teacher/TeacherTest.java
git commit -m "feat: implement equals, hashCode, and toString for Teacher"
```
