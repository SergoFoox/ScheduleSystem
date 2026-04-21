# План впровадження валідації узгодженості CoursePlan

> **Для агентних працівників:** ОБОВ'ЯЗКОВА ПІД-НАВИЧКА: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для поетапного виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Додати валідацію до `CoursePlan`, щоб переконатися, що `totalHours` відповідає сумі `lectureHours`, `practiceHours` та `labHours`.

**Архітектура:** Використання Bean Validation API (`@AssertTrue`) у доменній сутності.

**Технологічний стек:** Java 21, Spring Boot 4.0.5, Jakarta Bean Validation.

---

### Завдання 1: Оновлення CoursePlanTest.java

**Файли:**
- Змінити: `src/test/java/com/sergofoox/domain/plan/CoursePlanTest.java`

- [ ] **Крок 1: Оновлення перевірки повідомлення валідації у `testTotalHoursValidation` відповідно до вимог.**

Користувач запитав: "Total hours must equal the sum of lecture, practice, and lab hours"

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
            .anyMatch(v -> v.getMessage().equals("Total hours must equal the sum of lecture, practice, and lab hours"));
        assertTrue(hasConsistencyViolation, "Expected consistency violation message: 'Total hours must equal the sum of lecture, practice, and lab hours'");
    }
```

- [ ] **Крок 2: Запуск тесту для підтвердження його невдачі**

Виконати: `./mvnw test -Dtest=CoursePlanTest`
Очікується: НЕВДАЧА (все ще не вдається, оскільки валідація відсутня)

- [ ] **Крок 3: Коміт**

```bash
git add src/test/java/com/sergofoox/domain/plan/CoursePlanTest.java
git commit -m "test: update CoursePlanTest validation message"
```

---

### Завдання 2: Впровадження валідації у CoursePlan.java

**Файли:**
- Змінити: `src/main/java/com/sergofoox/domain/plan/CoursePlan.java`

- [ ] **Крок 1: Додати метод @AssertTrue до класу CoursePlan.**

```java
    @jakarta.validation.constraints.AssertTrue(message = "Total hours must equal the sum of lecture, practice, and lab hours")
    public boolean isHoursConsistent() {
        if (totalHours == null || lectureHours == null || practiceHours == null || labHours == null) {
            return true; // Нехай @NotNull обробляє значення null
        }
        return totalHours == (lectureHours + practiceHours + labHours);
    }
```

- [ ] **Крок 2: Додати імпорт для AssertTrue, якщо він ще не присутній.**

```java
import jakarta.validation.constraints.AssertTrue;
```

- [ ] **Крок 3: Запуск тесту для підтвердження його успіху**

Виконати: `./mvnw test -Dtest=CoursePlanTest`
Очікується: УСПІШНО

- [ ] **Крок 4: Коміт**

```bash
git add src/main/java/com/sergofoox/domain/plan/CoursePlan.java
git commit -m "feat: add hours consistency validation to CoursePlan"
```

---

### Завдання 3: Фінальна перевірка

- [ ] **Крок 1: Запуск усіх тестів у проекті.**

Виконати: `./mvnw test`
Очікується: ВСІ ПРОЙШЛИ

- [ ] **Крок 2: Резюме та завершення.**
