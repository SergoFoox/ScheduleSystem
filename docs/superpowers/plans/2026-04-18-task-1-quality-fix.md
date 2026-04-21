# План впровадження виправлення якості Завдання 1

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Виправлення впровадження Завдання 1 шляхом переміщення файлів до правильного доменного пакета, додавання валідації bean-компонентів, а також покращення тестового покриття та безпечного для JPA equals/hashCode.

**Архітектура:** Підхід проблемно-орієнтованого проектування (DDD) шляхом переміщення сутностей у доменні пакети. Jakarta Bean Validation для цілісності даних. Безпечний для JPA equals/hashCode на основі бізнес-ключів або узгодженої обробки ідентифікаторів.

**Технологічний стек:** Java 21, Spring Boot 4.0.5, Jakarta Persistence, Jakarta Validation, JUnit 5.

---

### Завдання 1: Переміщення та рефакторинг пакетів

**Файли:**
- Перемістити: `src/main/java/com/sergofoox/entity/Subject.java` -> `src/main/java/com/sergofoox/domain/subject/Subject.java`
- Перемістити: `src/main/java/com/sergofoox/entity/LessonType.java` -> `src/main/java/com/sergofoox/domain/subject/LessonType.java`
- Перемістити: `src/test/java/com/sergofoox/entity/SubjectTest.java` -> `src/test/java/com/sergofoox/domain/subject/SubjectTest.java`

- [ ] **Крок 1: Створення цільових каталогів**
Виконати: `mkdir -p src/main/java/com/sergofoox/domain/subject src/test/java/com/sergofoox/domain/subject`

- [ ] **Крок 2: Переміщення файлів**
Виконати: `mv src/main/java/com/sergofoox/entity/Subject.java src/main/java/com/sergofoox/domain/subject/`
Виконати: `mv src/main/java/com/sergofoox/entity/LessonType.java src/main/java/com/sergofoox/domain/subject/`
Виконати: `mv src/test/java/com/sergofoox/entity/SubjectTest.java src/test/java/com/sergofoox/domain/subject/`

- [ ] **Крок 3: Оновлення оголошень пакетів та імпортів**
Оновити `Subject.java`, `LessonType.java` та `SubjectTest.java` для використання `package com.sergofoox.domain.subject;`.

- [ ] **Крок 4: Перевірка компіляції проекту**
Виконати: `./mvnw compile`
Очікується: УСПІШНО

- [ ] **Крок 5: Коміт переміщення**
```bash
git add src/main/java/com/sergofoox/domain/subject src/test/java/com/sergofoox/domain/subject src/main/java/com/sergofoox/entity/ src/test/java/com/sergofoox/entity/
git commit -m "refactor: relocate Subject and LessonType to domain package"
```

### Завдання 2: Розширення SubjectTest за допомогою TDD (Червона фаза)

**Файли:**
- Змінити: `src/test/java/com/sergofoox/domain/subject/SubjectTest.java`

- [ ] **Крок 1: Додавання тестів для конструктора з усіма аргументами, equals та hashCode**
Оновити `SubjectTest.java` більш розлогими тестами.

```java
package com.sergofoox.domain.subject;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SubjectTest {
    @Test
    void testSubjectCreation() {
        Subject subject = new Subject("Mathematics", "Math");
        assertEquals("Mathematics", subject.getName());
        assertEquals("Math", subject.getAbbreviation());
    }

    @Test
    void testAllArgsConstructorAndGetters() {
        Subject subject = new Subject(1L, "Physics", "Phys");
        assertEquals(1L, subject.getId());
        assertEquals("Physics", subject.getName());
        assertEquals("Phys", subject.getAbbreviation());
    }

    @Test
    void testEqualsAndHashCode() {
        Subject s1 = new Subject(1L, "Math", "M");
        Subject s2 = new Subject(1L, "Math", "M");
        Subject s3 = new Subject(2L, "Physics", "P");
        Subject s4 = new Subject(null, "Math", "M");
        Subject s5 = new Subject(null, "Math", "M");

        // Reflexive
        assertEquals(s1, s1);
        
        // Symmetric
        assertEquals(s1, s2);
        assertEquals(s2, s1);
        
        // Consistent with hashCode
        assertEquals(s1.hashCode(), s2.hashCode());
        
        // Not equal
        assertNotEquals(s1, s3);
        assertNotEquals(s1, null);
        assertNotEquals(s1, new Object());

        // Null IDs equality (based on business key if possible, but let's check current impl first)
        // For JPA safety, we might want to compare business keys when ID is null.
        // For now, let's ensure consistency.
        assertEquals(s4, s5);
        assertEquals(s4.hashCode(), s5.hashCode());
    }
}
```

- [ ] **Крок 2: Додавання тестів валідації (спочатку не пройдуть)**
Потребує `jakarta.validation-api` та реалізацію (наприклад, Hibernate Validator) у `pom.xml`. Припускаємо, що вони там є, оскільки це проект Spring Boot.

```java
    @Test
    void testValidation() {
        // Це модульний тест, ми можемо використовувати Validator для перевірки анотацій
        jakarta.validation.ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
        jakarta.validation.Validator validator = factory.getValidator();

        Subject invalidSubject = new Subject("", "");
        var violations = validator.validate(invalidSubject);
        assertFalse(violations.isEmpty(), "Should have violations for empty strings");
        
        Subject validSubject = new Subject("Mathematics", "MATH");
        assertTrue(validator.validate(validSubject).isEmpty());
    }
```

- [ ] **Крок 3: Запуск тестів та перевірка невдачі**
Виконати: `./mvnw test -Dtest=SubjectTest`
Очікується: НЕВДАЧА (тест валідації не пройде, оскільки анотації відсутні)

### Завдання 3: Впровадження валідації та вдосконалення Equals/HashCode (Зелена фаза)

**Файли:**
- Змінити: `src/main/java/com/sergofoox/domain/subject/Subject.java`

- [ ] **Крок 1: Додавання анотацій валідації**
Додати `@NotBlank` та `@Size` до `name` та `abbreviation`.

- [ ] **Крок 2: Вдосконалення equals та hashCode для безпеки JPA**
Використовувати тільки `id` для `equals`/`hashCode`, якщо `id` присутній, або стабільний бізнес-ключ. Для цього завдання переконайтеся, що це відповідає очікуванням тесту.

- [ ] **Крок 3: Запуск тестів та перевірка успіху**
Виконати: `./mvnw test -Dtest=SubjectTest`
Очікується: УСПІШНО

- [ ] **Крок 4: Коміт виправлень**
```bash
git add src/main/java/com/sergofoox/domain/subject/Subject.java src/test/java/com/sergofoox/domain/subject/SubjectTest.java
git commit -m "feat: add validation and improve Subject entity quality"
```

### Завдання 4: Фінальна перевірка

- [ ] **Крок 1: Запуск усіх тестів у проекті**
Виконати: `./mvnw test`
Очікується: УСПІШНО

- [ ] **Крок 2: Перевірка на наявність залишкових посилань на старий пакет**
Виконати: `grep -r "com.sergofoox.entity" .`
Очікується: Збігів немає (крім, можливо, у target/ або .git/)
