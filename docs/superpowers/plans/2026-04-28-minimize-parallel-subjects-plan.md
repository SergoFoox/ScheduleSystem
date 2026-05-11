# Мінімізація паралельних занять з одного предмета - План реалізації

> **Для агентів:** НЕОБХІДНИЙ ПІД-СКІЛ: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для покрокового виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Впровадити м'яке обмеження в Timefold солвер, щоб мінімізувати кількість однакових предметів, що викладаються в той самий час для різних груп.

**Архітектура:** Додавання нового Soft Constraint в `ScheduleConstraintProvider`, який порівнює предмет, день тижня та номер пари між уроками.

**Стек технологій:** Java 21, Spring Boot, Timefold Solver.

---

### Завдання 1: Додавання тесту для нового обмеження

Ми почнемо з TDD і напишемо тест, який імітує ситуацію з двома однаковими предметами в один час. Зараз цей тест має проходити (як допустимий), але пізніше ми перевіримо, чи зменшується Soft Score.

**Файли:**
- Модифікувати: `src/test/java/com/sergofoox/domain/solver/ScheduleSolverTest.java`

- [ ] **Крок 1: Додати тестовий метод `prefersToSeparateSameSubjects`**

```java
    @Test
    void prefersToSeparateSameSubjects() {
        Timeslot t1 = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        t1.setId(1L);
        Timeslot t2 = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(10, 15), LocalTime.of(11, 45), 2);
        t2.setId(2L);

        Room r1 = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r1.setId(1L);
        Room r2 = new Room("102", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r2.setId(2L);

        Subject ukrainian = new Subject("Ukrainian", "U");
        ukrainian.setId(1L);

        Teacher teacher1 = new Teacher("Teacher One", "CS", PositionType.FULL_TIME);
        teacher1.setId(1L);
        Teacher teacher2 = new Teacher("Teacher Two", "CS", PositionType.FULL_TIME);
        teacher2.setId(2L);

        Group group1 = new Group("G-01", 20, 1, "CS");
        group1.setId(1L);
        Group group2 = new Group("G-02", 20, 1, "CS");
        group2.setId(2L);

        CoursePlan plan1 = new CoursePlan(ukrainian, teacher1, group1, 30, 15, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        plan1.setId(1L);
        CoursePlan plan2 = new CoursePlan(ukrainian, teacher2, group2, 30, 15, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL);
        plan2.setId(2L);

        Lesson lesson1 = new Lesson(ukrainian, LessonType.LECTURE, teacher1, group1, plan1);
        lesson1.setId(1L);
        Lesson lesson2 = new Lesson(ukrainian, LessonType.LECTURE, teacher2, group2, plan2);
        lesson2.setId(2L);

        Schedule problem = new Schedule(
                List.of(t1, t2),
                List.of(r1, r2),
                new ArrayList<>(List.of(lesson1, lesson2)));

        Schedule solution = solverFactory.buildSolver().solve(problem);

        assertTrue(solution.getScore().isFeasible(), "Розклад має бути реалістичним");
        
        // Після впровадження обмеження, уроки МАЮТЬ бути в різних слотах
        assertNotEquals(lessonById(solution, 1L).getTimeslot(), lessonById(solution, 2L).getTimeslot(),
                "Однакові предмети мають бути рознесені по часу, якщо є вільні слоти");
    }
```

- [ ] **Крок 2: Запустити тест та переконатися, що він ПАДАЄ**

Виконати: `./mvnw test -Dtest=ScheduleSolverTest#prefersToSeparateSameSubjects`
Очікуваний результат: FAIL (оскільки зараз солверу все одно, і він може поставити їх в один слот для швидкості).

### Завдання 2: Впровадження обмеження в ScheduleConstraintProvider

**Файли:**
- Модифікувати: `src/main/java/com/sergofoox/domain/solver/ScheduleConstraintProvider.java`

- [ ] **Крок 1: Додати виклик нового обмеження в метод `defineConstraints`**

```java
                // SOFT: ПРАВИЛА ІЗ ТЗ І РАСПРЕДЕЛЕНИЕ
                minimizeParallelSubjectLessons(constraintFactory), // Додати цей рядок
                assignedTeacherRoom(constraintFactory),
```

- [ ] **Крок 2: Реалізувати метод `minimizeParallelSubjectLessons`**

```java
    Constraint minimizeParallelSubjectLessons(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getSubject),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> weeksOverlap(l1, l2))
                .penalize(HardSoftScore.ofSoft(50))
                .asConstraint("Minimize parallel subject lessons");
    }
```

### Завдання 3: Верифікація та фіналізація

- [ ] **Крок 1: Запустити тест `prefersToSeparateSameSubjects` знову**

Виконати: `./mvnw test -Dtest=ScheduleSolverTest#prefersToSeparateSameSubjects`
Очікуваний результат: PASS

- [ ] **Крок 2: Запустити всі тести солвера**

Виконати: `./mvnw test -Dtest=ScheduleSolverTest`
Очікуваний результат: Усі тести проходять (BUILD SUCCESS).

- [ ] **Крок 3: Фіксація змін**

```bash
git add src/main/java/com/sergofoox/domain/solver/ScheduleConstraintProvider.java src/test/java/com/sergofoox/domain/solver/ScheduleSolverTest.java
git commit -m "feat: add soft constraint to minimize parallel lessons of the same subject"
```
