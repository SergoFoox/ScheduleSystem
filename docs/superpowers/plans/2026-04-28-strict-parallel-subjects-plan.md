# Жорстка заборона паралельних занять з одного предмета - План реалізації

> **Для агентів:** НЕОБХІДНИЙ ПІД-СКІЛ: Використовуйте superpowers:subagent-driven-development (рекомендовано) або superpowers:executing-plans для покрокового виконання цього плану. Кроки використовують синтаксис прапорців (`- [ ]`) для відстеження.

**Мета:** Впровадити ЖОРСТКЕ (HARD) обмеження в Timefold солвер, щоб повністю заборонити однаковий предмет у різних груп в один і той же час.

**Архітектура:** Додавання Hard Constraint в `ScheduleConstraintProvider`.

---

### Завдання 1: Оновлення тестів

**Файли:**
- Модифікувати: `src/test/java/com/sergofoox/domain/solver/ScheduleSolverTest.java`

- [ ] **Крок 1: Додати тест `strictlyAvoidsParallelSameSubjects`**

```java
    @Test
    void strictlyAvoidsParallelSameSubjects() {
        Timeslot timeslot = new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1);
        timeslot.setId(1L);

        Room r1 = new Room("101", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r1.setId(1L);
        Room r2 = new Room("102", 30, "Main", "Projector", RoomType.LECTURE_HALL);
        r2.setId(2L);

        Subject ukrainian = new Subject("Ukrainian", "U");
        ukrainian.setId(1L);

        Teacher t1 = new Teacher("Teacher One", "CS", PositionType.FULL_TIME); t1.setId(1L);
        Teacher t2 = new Teacher("Teacher Two", "CS", PositionType.FULL_TIME); t2.setId(2L);
        Group g1 = new Group("G-01", 20, 1, "CS"); g1.setId(1L);
        Group g2 = new Group("G-02", 20, 1, "CS"); g2.setId(2L);

        CoursePlan plan1 = new CoursePlan(ukrainian, t1, g1, 30, 15, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL); plan1.setId(1L);
        CoursePlan plan2 = new CoursePlan(ukrainian, t2, g2, 30, 15, 0, 0, 1, 0, 0, RoomType.LECTURE_HALL); plan2.setId(2L);

        Lesson lesson1 = new Lesson(ukrainian, LessonType.LECTURE, t1, g1, plan1); lesson1.setId(1L);
        Lesson lesson2 = new Lesson(ukrainian, LessonType.LECTURE, t2, g2, plan2); lesson2.setId(2L);

        // Тільки ОДИН таймслот на ДВА уроки з одного предмета
        Schedule problem = new Schedule(List.of(timeslot), List.of(r1, r2), new ArrayList<>(List.of(lesson1, lesson2)));

        Schedule solution = solverFactory.buildSolver().solve(problem);

        // Очікуємо, що розклад НЕ буде допустимим (feasible), бо один слот не може вмістити два однакових предмети за жорстким правилом
        assertFalse(solution.getScore().isFeasible(), "Розклад має бути НЕдопустимим, якщо однаковий предмет змушений бути в один час");
    }
```

### Завдання 2: Впровадження жорсткого обмеження

**Файли:**
- Модифікувати: `src/main/java/com/sergofoox/domain/solver/ScheduleConstraintProvider.java`

- [ ] **Крок 1: Додати виклик `subjectConflict` у секцію HARD методу `defineConstraints`**

```java
                // HARD: КРИТИЧЕСКИЕ (Нельзя нарушать)
                teacherConflict(constraintFactory),
                groupConflict(constraintFactory),
                splitGroupTimeslotSync(constraintFactory),
                roomConflict(constraintFactory),
                subjectConflict(constraintFactory), // Додати сюди
```

- [ ] **Крок 2: Реалізувати метод `subjectConflict` з високим HARD штрафом**

```java
    Constraint subjectConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getSubject),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(l -> l.getTimeslot() == null ? null : l.getTimeslot().getLessonNumber()))
                .filter((l1, l2) -> samePhysicalSlot(l1, l2) 
                        && weeksOverlap(l1, l2)
                        && !sameSplitGroupLesson(l1, l2))
                .penalize(HardSoftScore.ofHard(10000))
                .asConstraint("Subject conflict");
    }
```

### Завдання 3: Перевірка

- [ ] **Крок 1: Запустити всі тести**

Виконати: `./mvnw test -Dtest=ScheduleSolverTest`
Expected: BUILD SUCCESS.

- [ ] **Крок 2: Коміт**

```bash
git add src/main/java/com/sergofoox/domain/solver/ScheduleConstraintProvider.java src/test/java/com/sergofoox/domain/solver/ScheduleSolverTest.java
git commit -m "feat: enforce strict hard constraint for parallel subjects"
```
