# План впровадження виправлень доменів ASMS V3

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Виправлення сутностей Group, Subject та CoursePlan на основі огляду якості коду: стабільні бізнес-ключі для Group, зіставлення шаблонів (pattern matching) для рівності Subject та узгодженість загальної кількості годин для CoursePlan.

**Архітектура:** Точкові оновлення існуючих доменних сутностей та їх модульних тестів.

**Технологічний стек:** Java 21, Spring Boot 4.0.5, JUnit 5, Jakarta Validation.

---

### Завдання 1: Оновлення логіки рівності Group

**Файли:**
- Змінити: `src/main/java/com/sergofoox/domain/group/Group.java`
- Змінити: `src/test/java/com/sergofoox/domain/group/GroupTest.java`

- [ ] **Крок 1: Оновлення GroupTest.java тестом, що не проходить**
Оновити `testEquality`, щоб перевірити, що групи з різним курсом, але однаковою назвою та відділом, рівні.

```java
    @Test
    void testEquality() {
        Group g1 = new Group("KB-41", 25, 4, "CS");
        Group g2 = new Group("KB-41", 25, 3, "CS"); // Різний курс, все одно мають бути рівними
        assertEquals(g1, g2, "Groups with same name and department should be equal regardless of course");
        assertEquals(g1.hashCode(), g2.hashCode());
    }
```

- [ ] **Крок 2: Запуск тесту для підтвердження невдачі**
Виконати: `./mvnw test -Dtest=GroupTest`
Очікується: НЕВДАЧА

- [ ] **Крок 3: Оновлення логіки рівності Group.java**
Видалити `course` з `equals()` та `hashCode()`.

```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group group)) return false;
        return Objects.equals(name, group.name) &&
               Objects.equals(department, group.department);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, department);
    }
```

- [ ] **Крок 4: Запуск тесту для підтвердження успіху**
Виконати: `./mvnw test -Dtest=GroupTest`
Очікується: УСПІШНО

- [ ] **Крок 5: Коміт**
```bash
git add src/main/java/com/sergofoox/domain/group/Group.java src/test/java/com/sergofoox/domain/group/GroupTest.java
git commit -m "refactor(domain): update Group equality to use stable business keys (name, department)"
```

### Завдання 2: Оновлення зіставлення шаблонів рівності Subject

**Файли:**
- Змінити: `src/main/java/com/sergofoox/domain/subject/Subject.java`
- Тест: `src/test/java/com/sergofoox/domain/subject/SubjectTest.java`

- [ ] **Крок 1: Оновлення зіставлення шаблонів рівності Subject.java**
Використовувати зіставлення шаблонів для `instanceof` у `equals()`.

```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subject subject)) return false;
        return Objects.equals(name, subject.name) &&
               Objects.equals(abbreviation, subject.abbreviation);
    }
```

- [ ] **Крок 2: Запуск тесту для підтвердження успіху**
Виконати: `./mvnw test -Dtest=SubjectTest`
Очікується: УСПІШНО

- [ ] **Крок 3: Коміт**
```bash
git add src/main/java/com/sergofoox/domain/subject/Subject.java
git commit -m "style(domain): update Subject equality to use pattern matching for instanceof"
```

### Завдання 3: Додавання валідації узгодженості CoursePlan

**Файли:**
- Змінити: `src/main/java/com/sergofoox/domain/plan/CoursePlan.java`
- Змінити: `src/test/java/com/sergofoox/domain/plan/CoursePlanTest.java`

- [ ] **Крок 1: Додавання тесту валідації до CoursePlanTest.java**
Додати тестовий випадок, який перевіряє порушення валідації при неузгодженості загальної кількості годин.

```java
    @Test
    void testTotalHoursValidation() {
        jakarta.validation.ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
        jakarta.validation.Validator validator = factory.getValidator();

        Subject s = new Subject("Math", "M");
        Group g = new Group("KB-41", 25, 4, "CS");
        
        // Correct total hours
        CoursePlan validPlan = new CoursePlan(s, g, 120, 40, 40, 40, 1, 1, 1, RoomType.LECTURE_HALL);
        assertTrue(validator.validate(validPlan).isEmpty(), "Valid plan should have no violations");

        // Incorrect total hours (120 != 40 + 40 + 30)
        CoursePlan invalidPlan = new CoursePlan(s, g, 120, 40, 40, 30, 1, 1, 1, RoomType.LECTURE_HALL);
        var violations = validator.validate(invalidPlan);
        assertFalse(violations.isEmpty(), "Incorrect total hours should produce validation error");
        boolean hasConsistencyViolation = violations.stream()
            .anyMatch(v -> v.getMessageTemplate().contains("Total hours must match the sum of lecture, practice and lab hours"));
        assertTrue(hasConsistencyViolation, "Expected consistency violation message");
    }
```

- [ ] **Крок 2: Запуск тесту для підтвердження невдачі**
Виконати: `./mvnw test -Dtest=CoursePlanTest`
Очікується: НЕВДАЧА (на `hasConsistencyViolation`)

- [ ] **Крок 3: Додавання логіки валідації до CoursePlan.java**
Додати метод `@AssertTrue`.

```java
    @jakarta.validation.constraints.AssertTrue(message = "Total hours must match the sum of lecture, practice and lab hours")
    public boolean isTotalHoursConsistent() {
        if (totalHours == null || lectureHours == null || practiceHours == null || labHours == null) {
            return true; // Нехай @NotNull обробляє перевірки на null
        }
        return totalHours.equals(lectureHours + practiceHours + labHours);
    }
```

- [ ] **Крок 4: Запуск тесту для підтвердження успіху**
Виконати: `./mvnw test -Dtest=CoursePlanTest`
Очікується: УСПІШНО

- [ ] **Крок 5: Коміт**
```bash
git add src/main/java/com/sergofoox/domain/plan/CoursePlan.java src/test/java/com/sergofoox/domain/plan/CoursePlanTest.java
git commit -m "feat(domain): add consistency validation for CoursePlan total hours"
```
