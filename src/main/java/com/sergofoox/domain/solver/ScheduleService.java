package com.sergofoox.domain.solver;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.group.GroupRepository;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.plan.CoursePlan;
import com.sergofoox.domain.plan.CoursePlanRepository;
import com.sergofoox.domain.plan.Periodicity;
import com.sergofoox.domain.room.Room;
import com.sergofoox.domain.room.RoomRepository;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.SubjectRepository;
import com.sergofoox.domain.subject.LessonType;
import com.sergofoox.domain.teacher.AvailabilityStatus;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherAvailability;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.timeslot.Timeslot;
import com.sergofoox.domain.timeslot.TimeslotRepository;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final int ACADEMIC_HOURS_PER_LESSON = 2;
    private static final int PLANNING_WEEKS = 8;

    private final SolverManager<Schedule, UUID> solverManager;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final RoomRepository roomRepository;
    private final TimeslotRepository timeslotRepository;
    private final LessonRepository lessonRepository;
    private final CoursePlanRepository coursePlanRepository;
    private final SubjectRepository subjectRepository;
    private final TransactionTemplate transactionTemplate;
    private volatile Integer activeCourseFilter;

    public ScheduleService(SolverManager<Schedule, UUID> solverManager,
                           TeacherRepository teacherRepository,
                           GroupRepository groupRepository,
                           RoomRepository roomRepository,
                           TimeslotRepository timeslotRepository,
                           LessonRepository lessonRepository,
                           CoursePlanRepository coursePlanRepository,
                           SubjectRepository subjectRepository,
                           TransactionTemplate transactionTemplate) {
        this.solverManager = solverManager;
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.roomRepository = roomRepository;
        this.timeslotRepository = timeslotRepository;
        this.lessonRepository = lessonRepository;
        this.coursePlanRepository = coursePlanRepository;
        this.subjectRepository = subjectRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public void generateLessonsFromPlans() {
        generateLessonsFromPlans(null);
    }

    @Transactional
    public void generateLessonsFromPlans(Integer course) {
        System.out.println("=== ГЕНЕРАЦИЯ УРОКОВ ===");
        if (course == null) {
            lessonRepository.deleteAll();
        } else {
            List<Group> groupsForCourse = groupRepository.findAll().stream()
                    .filter(group -> course.equals(group.getCourse()))
                    .toList();
            if (groupsForCourse.isEmpty()) {
                throw new IllegalArgumentException("Немає груп для " + course + " курсу");
            }
            groupsForCourse.forEach(lessonRepository::deleteByGroup);
        }
        lessonRepository.flush();
        
        List<CoursePlan> allPlans = coursePlanRepository.findAll();
        List<CoursePlan> plansForGeneration = course == null
                ? allPlans
                : allPlans.stream()
                        .filter(plan -> plan.getGroup() != null && course.equals(plan.getGroup().getCourse()))
                        .toList();
        if (plansForGeneration.isEmpty()) {
            throw new IllegalStateException(course == null
                    ? "Немає навчальних планів для генерації"
                    : "Немає навчальних планів для " + course + " курсу");
        }
        Random generationRandom = new Random(System.nanoTime());
        List<CoursePlan> randomizedPlans = new ArrayList<>(plansForGeneration);
        Collections.shuffle(randomizedPlans, generationRandom);
        List<Lesson> newLessons = new ArrayList<>();
        Map<Object, BiWeeklyGenerationState> biWeeklyStateByGroup = new HashMap<>();

        for (CoursePlan plan : randomizedPlans) {
            if (getPrimaryTeacher(plan) == null) {
                System.out.println("Skipping course plan without teacher: id=" + plan.getId());
                continue;
            }

            // Генерируем уроки согласно часам и периодичности из плана
            BiWeeklyGenerationState biWeeklyState = biWeeklyStateByGroup.computeIfAbsent(groupGenerationKey(plan),
                    ignored -> new BiWeeklyGenerationState());
            for (GeneratedLessonSpec spec : buildLessonSpecs(plan, biWeeklyState)) {
                addLessonsForPlan(newLessons, plan, spec.lessonType(), spec.periodicity(), spec.splitGroupIndex());
            }
        }
        if (newLessons.isEmpty() && !plansForGeneration.isEmpty()) {
            throw new IllegalStateException("No lessons were generated. Check that course plans have teachers and planned hours.");
        }
        Collections.shuffle(newLessons, generationRandom);
        logTeacherLoadWarnings(newLessons);
        System.out.println("Создано уроков: " + newLessons.size());
        lessonRepository.saveAll(newLessons);
        lessonRepository.flush();
    }

    private Object groupGenerationKey(CoursePlan plan) {
        if (plan.getGroup() == null) {
            return plan;
        }
        return plan.getGroup().getId() != null ? plan.getGroup().getId() : plan.getGroup();
    }

    private void addLessonsForPlan(List<Lesson> lessons, CoursePlan plan, LessonType lessonType, com.sergofoox.domain.plan.Periodicity periodicity, int splitGroupIndex) {
        Teacher primaryTeacher = getPrimaryTeacher(plan);
        if (primaryTeacher == null) {
            return;
        }

        if (plan.getTeacher() != null && plan.getSecondTeacher() != null) {
            Lesson firstSubgroup = new Lesson(plan.getSubject(), lessonType, primaryTeacher, plan.getGroup(), plan, 1);
            firstSubgroup.setPeriodicity(periodicity);
            firstSubgroup.setSplitGroupIndex(splitGroupIndex);
            lessons.add(firstSubgroup);

            Lesson secondSubgroup = new Lesson(plan.getSubject(), lessonType, plan.getSecondTeacher(), plan.getGroup(), plan, 2);
            secondSubgroup.setPeriodicity(periodicity);
            secondSubgroup.setSplitGroupIndex(splitGroupIndex);
            lessons.add(secondSubgroup);
            return;
        }

        Lesson lesson = new Lesson(plan.getSubject(), lessonType, primaryTeacher, plan.getGroup(), plan);
        lesson.setPeriodicity(periodicity);
        lesson.setSplitGroupIndex(splitGroupIndex);
        lessons.add(lesson);
    }

    static List<GeneratedLessonSpec> buildLessonSpecs(CoursePlan plan) {
        return buildLessonSpecs(plan, new BiWeeklyGenerationState());
    }

    static List<GeneratedLessonSpec> buildLessonSpecs(CoursePlan plan, BiWeeklyGenerationState biWeeklyState) {
        List<GeneratedLessonSpec> specs = new ArrayList<>();
        BiWeeklyPairTracker pairTracker = new BiWeeklyPairTracker();

        addLessonSpecs(specs, LessonType.LECTURE, plan.getLectureHours(), plan.getLecturePeriodicity(), biWeeklyState, pairTracker);
        addLessonSpecs(specs, LessonType.PRACTICE, plan.getPracticeHours(), plan.getPracticePeriodicity(), biWeeklyState, pairTracker);
        addLessonSpecs(specs, LessonType.LABORATORY, plan.getLabHours(), plan.getLabPeriodicity(), biWeeklyState, pairTracker);

        return specs;
    }

    private static void addLessonSpecs(List<GeneratedLessonSpec> specs,
                                       LessonType lessonType,
                                       Integer academicHours,
                                       Periodicity preferredPeriodicity,
                                       BiWeeklyGenerationState biWeeklyState,
                                       BiWeeklyPairTracker pairTracker) {
        int sessionCount = sessionsFromHours(academicHours);
        int fullWeeklyCount = sessionCount / PLANNING_WEEKS;
        int remainingSessions = sessionCount % PLANNING_WEEKS;

        Set<Integer> usedSplitGroupIndexes = new HashSet<>();
        int splitGroupIndex = 1;
        for (int i = 0; i < fullWeeklyCount; i++) {
            specs.add(new GeneratedLessonSpec(lessonType, Periodicity.WEEKLY, splitGroupIndex++));
            usedSplitGroupIndexes.add(i + 1);
        }

        if (remainingSessions == 0) {
            return;
        }

        Periodicity periodicity = remainingSessions <= PLANNING_WEEKS / 2
                ? chooseBiWeeklyPeriodicity(preferredPeriodicity, biWeeklyState)
                : Periodicity.WEEKLY;
        int resolvedSplitGroupIndex = periodicity == Periodicity.WEEKLY
                ? splitGroupIndex
                : pairTracker.indexFor(periodicity, usedSplitGroupIndexes);
        specs.add(new GeneratedLessonSpec(lessonType, periodicity, resolvedSplitGroupIndex));
    }

    private static int sessionsFromHours(Integer academicHours) {
        if (academicHours == null || academicHours <= 0) {
            return 0;
        }
        return (academicHours + ACADEMIC_HOURS_PER_LESSON - 1) / ACADEMIC_HOURS_PER_LESSON;
    }

    private static Periodicity chooseBiWeeklyPeriodicity(Periodicity preferredPeriodicity, BiWeeklyGenerationState biWeeklyState) {
        if (preferredPeriodicity == Periodicity.ODD_WEEKS || preferredPeriodicity == Periodicity.EVEN_WEEKS) {
            return preferredPeriodicity;
        }
        return biWeeklyState.next();
    }

    record GeneratedLessonSpec(LessonType lessonType, Periodicity periodicity, int splitGroupIndex) {
    }

    static final class BiWeeklyGenerationState {
        private Periodicity nextBiWeekly = Periodicity.ODD_WEEKS;

        Periodicity next() {
            Periodicity selected = nextBiWeekly;
            nextBiWeekly = selected == Periodicity.ODD_WEEKS ? Periodicity.EVEN_WEEKS : Periodicity.ODD_WEEKS;
            return selected;
        }
    }

    private static final class BiWeeklyPairTracker {
        private final Deque<Integer> openOddIndexes = new ArrayDeque<>();
        private final Deque<Integer> openEvenIndexes = new ArrayDeque<>();
        private int nextIndex = 1;

        int indexFor(Periodicity periodicity, Set<Integer> usedSplitGroupIndexes) {
            Deque<Integer> oppositeIndexes = periodicity == Periodicity.ODD_WEEKS ? openEvenIndexes : openOddIndexes;
            Integer pairedIndex = pollCompatibleIndex(oppositeIndexes, usedSplitGroupIndexes);
            if (pairedIndex != null) {
                return pairedIndex;
            }

            int newIndex = nextAvailableIndex(usedSplitGroupIndexes);
            Deque<Integer> sameParityIndexes = periodicity == Periodicity.ODD_WEEKS ? openOddIndexes : openEvenIndexes;
            sameParityIndexes.addLast(newIndex);
            return newIndex;
        }

        private Integer pollCompatibleIndex(Deque<Integer> indexes, Set<Integer> usedSplitGroupIndexes) {
            Iterator<Integer> iterator = indexes.iterator();
            while (iterator.hasNext()) {
                Integer candidate = iterator.next();
                if (!usedSplitGroupIndexes.contains(candidate)) {
                    iterator.remove();
                    return candidate;
                }
            }
            return null;
        }

        private int nextAvailableIndex(Set<Integer> usedSplitGroupIndexes) {
            while (usedSplitGroupIndexes.contains(nextIndex)) {
                nextIndex++;
            }
            return nextIndex++;
        }
    }

    private void logTeacherLoadWarnings(List<Lesson> lessons) {
        Map<Long, TeacherLoadSummary> loadByTeacher = new LinkedHashMap<>();
        for (Lesson lesson : lessons) {
            Teacher teacher = lesson.getTeacher();
            if (teacher == null || teacher.getId() == null) {
                continue;
            }
            TeacherLoadSummary load = loadByTeacher.computeIfAbsent(teacher.getId(), id ->
                    new TeacherLoadSummary(teacher.getFullName(), teacher.getWeeklyHourLimit()));
            Periodicity periodicity = lesson.getPeriodicity();
            if (periodicity == Periodicity.WEEKLY || periodicity == Periodicity.ODD_WEEKS) {
                load.oddWeekHours += ACADEMIC_HOURS_PER_LESSON;
            }
            if (periodicity == Periodicity.WEEKLY || periodicity == Periodicity.EVEN_WEEKS) {
                load.evenWeekHours += ACADEMIC_HOURS_PER_LESSON;
            }
        }

        for (TeacherLoadSummary load : loadByTeacher.values()) {
            if (load.weeklyHourLimit != null
                    && (load.oddWeekHours > load.weeklyHourLimit || load.evenWeekHours > load.weeklyHourLimit)) {
                System.out.println("Warning: teacher weekly hour limit exceeded before solving: "
                        + load.teacherName
                        + ", limit=" + load.weeklyHourLimit
                        + ", oddWeekHours=" + load.oddWeekHours
                        + ", evenWeekHours=" + load.evenWeekHours);
            }
        }
    }

    private static final class TeacherLoadSummary {
        private final String teacherName;
        private final Integer weeklyHourLimit;
        private int oddWeekHours;
        private int evenWeekHours;

        private TeacherLoadSummary(String teacherName, Integer weeklyHourLimit) {
            this.teacherName = teacherName;
            this.weeklyHourLimit = weeklyHourLimit;
        }
    }

    private Teacher getPrimaryTeacher(CoursePlan plan) {
        return plan.getTeacher() != null ? plan.getTeacher() : plan.getSecondTeacher();
    }

    private boolean isEnglishSubject(CoursePlan plan) {
        String name = plan.getSubject() != null && plan.getSubject().getName() != null
                ? plan.getSubject().getName().toLowerCase(Locale.ROOT)
                : "";
        String abbreviation = plan.getSubject() != null && plan.getSubject().getAbbreviation() != null
                ? plan.getSubject().getAbbreviation().toLowerCase(Locale.ROOT)
                : "";
        return name.contains("англ") || name.contains("english") || abbreviation.contains("англ") || abbreviation.contains("eng");
    }

    @Transactional
    @SuppressWarnings("deprecation")
    public void solve() {
        solve(null);
    }

    @Transactional
    @SuppressWarnings("deprecation")
    public void solve(Integer course) {
        activeCourseFilter = course;
        if (solverManager.getSolverStatus(SINGLETON_ID) != SolverStatus.NOT_SOLVING) {
            solverManager.terminateEarly(SINGLETON_ID);
        }
        solverManager.solveAndListen(
                SINGLETON_ID,
                this::findById,
                this::saveProgressSolution,
                this::saveFinalSolution,
                (problemId, throwable) -> throwable.printStackTrace());
    }

    public Schedule findById(UUID id) {
        Integer courseFilter = activeCourseFilter;
        // Загружаем все справочники
        List<Room> rooms = roomRepository.findAll();
        List<Timeslot> timeslots = timeslotRepository.findAll();
        List<Teacher> teachers = teacherRepository.findAll();
        List<Group> groups = groupRepository.findAll();
        List<Subject> subjects = subjectRepository.findAll();
        List<CoursePlan> plans = coursePlanRepository.findAll();
        List<Lesson> lessons = lessonRepository.findAll();

        // Создаем карты для быстрой унификации объектов (чтобы ссылки в памяти совпадали)
        Map<Long, Room> roomMap = rooms.stream().collect(Collectors.toMap(Room::getId, Function.identity()));
        Map<Long, Timeslot> timeslotMap = timeslots.stream().collect(Collectors.toMap(Timeslot::getId, Function.identity()));
        Map<Long, Teacher> teacherMap = teachers.stream().collect(Collectors.toMap(Teacher::getId, Function.identity()));
        Map<Long, Group> groupMap = groups.stream().collect(Collectors.toMap(Group::getId, Function.identity()));
        Map<Long, Subject> subjectMap = subjects.stream().collect(Collectors.toMap(Subject::getId, Function.identity()));
        Map<Long, CoursePlan> planMap = plans.stream().collect(Collectors.toMap(CoursePlan::getId, Function.identity()));

        for (Teacher teacher : teachers) {
            if (teacher.getAssignedRoom() != null) {
                teacher.setAssignedRoom(roomMap.get(teacher.getAssignedRoom().getId()));
            }
        }

        // Принудительная прошивка ссылок
        for (Lesson lesson : lessons) {
            if (lesson.getRoom() != null) lesson.setRoom(roomMap.get(lesson.getRoom().getId()));
            if (lesson.getTimeslot() != null) lesson.setTimeslot(timeslotMap.get(lesson.getTimeslot().getId()));
            lesson.setTeacher(teacherMap.get(lesson.getTeacher().getId()));
            lesson.setGroup(groupMap.get(lesson.getGroup().getId()));
            lesson.setSubject(subjectMap.get(lesson.getSubject().getId()));
            lesson.setCoursePlan(planMap.get(lesson.getCoursePlan().getId()));
            lesson.setPinned(courseFilter != null && !isLessonInCourse(lesson, courseFilter));
        }

        // Перемешиваем для рандома
        if (courseFilter != null) {
            lessons = lessons.stream()
                    .filter(lesson -> isLessonInCourse(lesson, courseFilter) || isScheduled(lesson))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        Random solverRandom = new Random(System.nanoTime());
        Collections.shuffle(lessons, solverRandom);
        Collections.shuffle(timeslots, solverRandom);
        Collections.shuffle(rooms, solverRandom);

        return new Schedule(timeslots, rooms, lessons);
    }

    private boolean isLessonInCourse(Lesson lesson, Integer course) {
        return lesson.getGroup() != null && course.equals(lesson.getGroup().getCourse());
    }

    private boolean isScheduled(Lesson lesson) {
        return lesson.getTimeslot() != null && lesson.getRoom() != null;
    }

    public void saveSolution(Schedule schedule) {
        saveFinalSolution(schedule);
    }

    public void saveProgressSolution(Schedule schedule) {
        transactionTemplate.executeWithoutResult(status -> persistSolution(schedule, false));
    }

    public void saveFinalSolution(Schedule schedule) {
        transactionTemplate.executeWithoutResult(status -> persistSolution(schedule, true));
    }

    private void persistSolution(Schedule schedule, boolean finalSolution) {
        System.out.println((finalSolution ? "Найдено финальное решение. Score: " : "Найдено промежуточное улучшение. Score: ")
                + schedule.getScore());
        Set<Long> lessonsToUnschedule = finalSolution
                ? findFinalConflictLessonIds(schedule.getLessons())
                : Collections.emptySet();
        if (!lessonsToUnschedule.isEmpty()) {
            System.out.println("Unscheduling lessons with remaining visible hard conflicts: " + lessonsToUnschedule.size());
        }
        int savedCount = 0;
        for (Lesson lesson : schedule.getLessons()) {
            if (lesson.getId() != null) {
                lessonRepository.findById(lesson.getId()).ifPresent(dbLesson -> {
                    boolean unschedule = lessonsToUnschedule.contains(lesson.getId());
                    Timeslot timeslot = !unschedule && lesson.getTimeslot() != null
                            ? timeslotRepository.getReferenceById(lesson.getTimeslot().getId())
                            : null;
                    Room room = !unschedule && lesson.getRoom() != null
                            ? roomRepository.getReferenceById(lesson.getRoom().getId())
                            : null;
                    dbLesson.setTimeslot(timeslot);
                    dbLesson.setRoom(room);
                    lessonRepository.save(dbLesson);
                });
                savedCount++;
            }
        }
        lessonRepository.flush();
        System.out.println("Saved scheduled lessons: " + savedCount);
    }

    static Set<Long> findFinalConflictLessonIds(List<Lesson> lessons) {
        Set<Long> lessonIds = new HashSet<>();

        for (Lesson lesson : lessons) {
            if (lesson.getId() != null && hasTeacherAvailability(lesson, AvailabilityStatus.UNAVAILABLE)) {
                lessonIds.add(lesson.getId());
            }
        }

        boolean changed;
        do {
            changed = false;
            for (int i = 0; i < lessons.size(); i++) {
                Lesson first = lessons.get(i);
                if (!canCheckVisibleConflict(first) || lessonIds.contains(first.getId())) {
                    continue;
                }
                for (int j = i + 1; j < lessons.size(); j++) {
                    Lesson second = lessons.get(j);
                    if (!canCheckVisibleConflict(second) || lessonIds.contains(second.getId())) {
                        continue;
                    }
                    if (hasVisibleHardConflict(first, second)) {
                        Lesson lessonToUnschedule = chooseConflictLoser(first, second);
                        lessonIds.add(lessonToUnschedule.getId());
                        changed = true;
                        break;
                    }
                }
                if (changed) {
                    break;
                }
            }
        } while (changed);

        return lessonIds;
    }

    private static boolean canCheckVisibleConflict(Lesson lesson) {
        return lesson.getId() != null
                && lesson.getTimeslot() != null;
    }

    private static boolean hasVisibleHardConflict(Lesson first, Lesson second) {
        if (first.getTimeslot() == null || second.getTimeslot() == null) {
            return false;
        }

        if (sameGroupSubjectDay(first, second) && !sameSplitGroupLesson(first, second)) {
            return true;
        }

        if (!samePhysicalSlot(first, second)) {
            return false;
        }

        if (sameSubject(first, second) && !sameSplitGroupLesson(first, second)) {
            return true;
        }

        if (!weeksOverlap(first, second)) {
            return false;
        }

        if (sameTeacher(first, second)) {
            return true;
        }

        if (sameGroup(first, second) && !sameSplitGroupLesson(first, second)) {
            return true;
        }

        boolean roomConflict = sameRoom(first, second);
        if (roomConflict && first.getRoom() != null && first.getRoom().getType() == com.sergofoox.domain.plan.RoomType.SPORTS_HALL) {
            roomConflict = false; // Sports halls allow multiple groups
        }
        if (roomConflict && sameSplitGroupLesson(first, second)) {
            boolean t1SameRoom = first.getTeacher() != null && first.getTeacher().getAssignedRoom() != null && first.getRoom() != null && first.getRoom().getId().equals(first.getTeacher().getAssignedRoom().getId());
            boolean t2SameRoom = second.getTeacher() != null && second.getTeacher().getAssignedRoom() != null && second.getRoom() != null && second.getRoom().getId().equals(second.getTeacher().getAssignedRoom().getId());
            if (t1SameRoom && t2SameRoom) {
                roomConflict = false;
            }
        }
        if (roomConflict) {
            return true;
        }
        
        return false;
    }

    private static boolean removeInternalWindowConflict(List<Lesson> lessons, Set<Long> removedLessonIds) {
        return removeInternalWindowConflict(lessons, removedLessonIds, true)
                || removeInternalWindowConflict(lessons, removedLessonIds, false);
    }

    private static boolean removeInternalWindowConflict(List<Lesson> lessons, Set<Long> removedLessonIds, boolean oddWeek) {
        Map<GroupDayKey, List<Lesson>> lessonsByGroupDay = new LinkedHashMap<>();
        for (Lesson lesson : lessons) {
            if (!canCheckVisibleConflict(lesson)
                    || removedLessonIds.contains(lesson.getId())
                    || lesson.getGroup() == null
                    || lesson.getGroup().getId() == null) {
                continue;
            }
            if (oddWeek ? !countsInOddWeek(lesson) : !countsInEvenWeek(lesson)) {
                continue;
            }
            GroupDayKey key = new GroupDayKey(lesson.getGroup().getId(), lesson.getTimeslot().getDayOfWeek());
            lessonsByGroupDay.computeIfAbsent(key, ignored -> new ArrayList<>()).add(lesson);
        }

        for (List<Lesson> dayLessons : lessonsByGroupDay.values()) {
            if (internalWindowCount(dayLessons) > 0) {
                Lesson lessonToUnschedule = chooseWindowConflictLoser(dayLessons);
                if (lessonToUnschedule != null) {
                    removedLessonIds.add(lessonToUnschedule.getId());
                    return true;
                }
            }
        }
        return false;
    }

    private static Lesson chooseWindowConflictLoser(List<Lesson> dayLessons) {
        Lesson best = null;
        int bestRemainingWindowCount = Integer.MAX_VALUE;
        for (Lesson candidate : dayLessons) {
            int remainingWindowCount = internalWindowCount(dayLessons, candidate);
            if (best == null
                    || isBetterWindowLoser(candidate, remainingWindowCount, best, bestRemainingWindowCount)) {
                best = candidate;
                bestRemainingWindowCount = remainingWindowCount;
            }
        }
        return best;
    }

    private static boolean isBetterWindowLoser(Lesson candidate,
                                               int candidateRemainingWindowCount,
                                               Lesson current,
                                               int currentRemainingWindowCount) {
        if (candidate.isPinned() != current.isPinned()) {
            return !candidate.isPinned();
        }
        if (candidateRemainingWindowCount != currentRemainingWindowCount) {
            return candidateRemainingWindowCount < currentRemainingWindowCount;
        }
        boolean candidatePreferred = hasTeacherAvailability(candidate, AvailabilityStatus.PREFERRED);
        boolean currentPreferred = hasTeacherAvailability(current, AvailabilityStatus.PREFERRED);
        if (candidatePreferred != currentPreferred) {
            return !candidatePreferred;
        }
        return candidate.getId() > current.getId();
    }

    private static int internalWindowCount(List<Lesson> dayLessons) {
        return internalWindowCount(dayLessons, null);
    }

    private static int internalWindowCount(List<Lesson> dayLessons, Lesson excludedLesson) {
        Set<Integer> lessonNumbers = new HashSet<>();
        Long excludedId = excludedLesson == null ? null : excludedLesson.getId();
        for (Lesson lesson : dayLessons) {
            if (lesson.getTimeslot() == null || sameId(lesson.getId(), excludedId)) {
                continue;
            }
            lessonNumbers.add(lesson.getTimeslot().getLessonNumber());
        }
        if (lessonNumbers.isEmpty()) {
            return 0;
        }
        int secondThirdPairWindowCount = secondThirdPairWindowCount(lessonNumbers);
        if (lessonNumbers.size() <= 1) {
            return secondThirdPairWindowCount;
        }
        int minLesson = Collections.min(lessonNumbers);
        int maxLesson = Collections.max(lessonNumbers);
        int internalWindowCount = Math.max(0, (maxLesson - minLesson + 1) - lessonNumbers.size());
        return Math.max(internalWindowCount, secondThirdPairWindowCount);
    }

    private static int secondThirdPairWindowCount(Set<Integer> lessonNumbers) {
        int windowCount = 0;
        if (isForbiddenSecondThirdPairGapAt(lessonNumbers, 2)) {
            windowCount++;
        }
        if (isForbiddenSecondThirdPairGapAt(lessonNumbers, 3)) {
            windowCount++;
        }
        return windowCount;
    }

    private static boolean isForbiddenSecondThirdPairGapAt(Set<Integer> lessonNumbers, int lessonNumber) {
        return !lessonNumbers.contains(lessonNumber)
                && lessonNumbers.stream().anyMatch(number -> number < lessonNumber)
                && lessonNumbers.stream().anyMatch(number -> number > lessonNumber);
    }

    private static boolean sameGroupSubjectDay(Lesson first, Lesson second) {
        return sameGroup(first, second)
                && sameSubject(first, second)
                && first.getTimeslot() != null
                && second.getTimeslot() != null
                && first.getTimeslot().getDayOfWeek() == second.getTimeslot().getDayOfWeek();
    }

    private static boolean sameTeacher(Lesson first, Lesson second) {
        return first.getTeacher() != null
                && second.getTeacher() != null
                && first.getTeacher().getId() != null
                && first.getTeacher().getId().equals(second.getTeacher().getId());
    }

    private static boolean sameRoom(Lesson first, Lesson second) {
        return first.getRoom() != null
                && second.getRoom() != null
                && first.getRoom().getId() != null
                && first.getRoom().getId().equals(second.getRoom().getId());
    }

    private static boolean sameSubject(Lesson first, Lesson second) {
        return first.getSubject() != null
                && second.getSubject() != null
                && first.getSubject().getId() != null
                && first.getSubject().getId().equals(second.getSubject().getId());
    }

    private static boolean sameGroup(Lesson first, Lesson second) {
        return first.getGroup() != null
                && second.getGroup() != null
                && first.getGroup().getId() != null
                && first.getGroup().getId().equals(second.getGroup().getId());
    }

    private static boolean isDuplicateAlternatingSubject(Lesson first, Lesson second) {
        return samePhysicalSlot(first, second)
                && sameGroup(first, second)
                && sameSubject(first, second)
                && isComplementaryBiWeekly(first, second)
                && !sameSplitGroupLesson(first, second);
    }

    private static boolean isComplementaryBiWeekly(Lesson first, Lesson second) {
        Periodicity firstPeriodicity = first.getPeriodicity();
        Periodicity secondPeriodicity = second.getPeriodicity();
        return (firstPeriodicity == Periodicity.ODD_WEEKS && secondPeriodicity == Periodicity.EVEN_WEEKS)
                || (firstPeriodicity == Periodicity.EVEN_WEEKS && secondPeriodicity == Periodicity.ODD_WEEKS);
    }

    private static boolean samePhysicalSlot(Lesson first, Lesson second) {
        return first.getTimeslot().getDayOfWeek() == second.getTimeslot().getDayOfWeek()
                && first.getTimeslot().getLessonNumber().equals(second.getTimeslot().getLessonNumber());
    }

    private static boolean weeksOverlap(Lesson first, Lesson second) {
        Periodicity firstPeriodicity = effectivePeriodicity(first);
        Periodicity secondPeriodicity = effectivePeriodicity(second);
        return firstPeriodicity == Periodicity.WEEKLY
                || secondPeriodicity == Periodicity.WEEKLY
                || firstPeriodicity == secondPeriodicity;
    }

    private static boolean sameSplitGroupLesson(Lesson first, Lesson second) {
        if (first.getGroup() == null || second.getGroup() == null
                || first.getCoursePlan() == null || second.getCoursePlan() == null
                || first.getSubgroup() == null || second.getSubgroup() == null
                || first.getSplitGroupIndex() == null || second.getSplitGroupIndex() == null) {
            return false;
        }
        return sameId(first.getGroup().getId(), second.getGroup().getId())
                && sameId(first.getCoursePlan().getId(), second.getCoursePlan().getId())
                && first.getLessonType() == second.getLessonType()
                && first.getSplitGroupIndex().equals(second.getSplitGroupIndex())
                && first.getSubgroup() > 0
                && second.getSubgroup() > 0
                && !first.getSubgroup().equals(second.getSubgroup());
    }

    private static Periodicity effectivePeriodicity(Lesson lesson) {
        if (lesson.getTimeslot() != null && lesson.getTimeslot().getWeekParity() != Periodicity.WEEKLY) {
            return lesson.getTimeslot().getWeekParity();
        }
        return lesson.getPeriodicity();
    }

    private static boolean countsInOddWeek(Lesson lesson) {
        Periodicity periodicity = effectivePeriodicity(lesson);
        return periodicity == Periodicity.WEEKLY || periodicity == Periodicity.ODD_WEEKS;
    }

    private static boolean countsInEvenWeek(Lesson lesson) {
        Periodicity periodicity = effectivePeriodicity(lesson);
        return periodicity == Periodicity.WEEKLY || periodicity == Periodicity.EVEN_WEEKS;
    }

    private static Lesson chooseConflictLoser(Lesson first, Lesson second) {
        if (first.isPinned() && !second.isPinned()) {
            return second;
        }
        if (second.isPinned() && !first.isPinned()) {
            return first;
        }
        boolean firstPreferred = hasTeacherAvailability(first, AvailabilityStatus.PREFERRED);
        boolean secondPreferred = hasTeacherAvailability(second, AvailabilityStatus.PREFERRED);
        if (firstPreferred && !secondPreferred) {
            return second;
        }
        if (secondPreferred && !firstPreferred) {
            return first;
        }
        return first.getId() > second.getId() ? first : second;
    }

    private static boolean hasTeacherAvailability(Lesson lesson, AvailabilityStatus status) {
        if (lesson.getTeacher() == null || lesson.getTimeslot() == null) {
            return false;
        }
        List<TeacherAvailability> availability = lesson.getTeacher().getAvailability();
        if (availability == null || availability.isEmpty()) {
            return false;
        }
        return availability.stream()
                .anyMatch(item -> item.getStatus() == status
                        && item.getDayOfWeek() == lesson.getTimeslot().getDayOfWeek()
                        && item.getLessonNumber().equals(lesson.getTimeslot().getLessonNumber()));
    }

    private static boolean sameId(Long firstId, Long secondId) {
        return firstId != null && firstId.equals(secondId);
    }

    private record GroupDayKey(Long groupId, DayOfWeek dayOfWeek) {
    }

    public SolverStatus getSolverStatus() { return solverManager.getSolverStatus(SINGLETON_ID); }
    public void stopSolving() { solverManager.terminateEarly(SINGLETON_ID); }
}
