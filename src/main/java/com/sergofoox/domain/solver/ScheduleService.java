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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

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

    /**
     * Regenerates unscheduled lesson entities from course plans.
     * <p>
     * Lesson counts are taken from the per-week session fields on {@link CoursePlan};
     * hour fields are plan metadata and do not automatically change periodicity.
     * Each generated lesson copies the periodicity configured for its lesson type.
     *
     * @param course optional course filter; when present, only groups from that
     *               course are cleared and regenerated
     */
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
        List<Lesson> newLessons = new ArrayList<>();

        for (CoursePlan plan : plansForGeneration) {
            if (getPrimaryTeacher(plan) == null) {
                System.out.println("Skipping course plan without teacher: id=" + plan.getId());
                continue;
            }

            // Generate lessons from the session counts and periodicity configured in the plan.
            for (int i = 0; i < plan.getLectureSessionsPerWeek(); i++) {
                addLessonsForPlan(newLessons, plan, LessonType.LECTURE, plan.getLecturePeriodicity(), i + 1);
            }
            for (int i = 0; i < plan.getPracticeSessionsPerWeek(); i++) {
                addLessonsForPlan(newLessons, plan, LessonType.PRACTICE, plan.getPracticePeriodicity(), i + 1);
            }
            for (int i = 0; i < plan.getLabSessionsPerWeek(); i++) {
                addLessonsForPlan(newLessons, plan, LessonType.LABORATORY, plan.getLabPeriodicity(), i + 1);
            }
        }
        if (newLessons.isEmpty() && !plansForGeneration.isEmpty()) {
            throw new IllegalStateException("No lessons were generated. Check that course plans have teachers and weekly sessions.");
        }
        System.out.println("Создано уроков: " + newLessons.size());
        lessonRepository.saveAll(newLessons);
        lessonRepository.flush();
    }

    /**
     * Adds one planned weekly session to the generated lesson list.
     * <p>
     * If a course plan has two teachers, the session becomes two subgroup
     * lessons with the same split-group index; otherwise it becomes one
     * whole-group lesson.
     */
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
                this::saveSolution,
                this::saveSolution,
                (problemId, throwable) -> throwable.printStackTrace());
    }

    public Schedule findById(UUID id) {
        Integer courseFilter = activeCourseFilter;
        // Load all reference data.
        List<Room> rooms = roomRepository.findAll();
        List<Timeslot> timeslots = timeslotRepository.findAll();
        List<Teacher> teachers = teacherRepository.findAll();
        List<Group> groups = groupRepository.findAll();
        List<Subject> subjects = subjectRepository.findAll();
        List<CoursePlan> plans = coursePlanRepository.findAll();
        List<Lesson> lessons = lessonRepository.findAll();

        // Build maps to reuse entity instances and keep in-memory references consistent.
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

        // Force all lesson references to point to the unified entity instances.
        for (Lesson lesson : lessons) {
            if (lesson.getRoom() != null) lesson.setRoom(roomMap.get(lesson.getRoom().getId()));
            if (lesson.getTimeslot() != null) lesson.setTimeslot(timeslotMap.get(lesson.getTimeslot().getId()));
            lesson.setTeacher(teacherMap.get(lesson.getTeacher().getId()));
            lesson.setGroup(groupMap.get(lesson.getGroup().getId()));
            lesson.setSubject(subjectMap.get(lesson.getSubject().getId()));
            lesson.setCoursePlan(planMap.get(lesson.getCoursePlan().getId()));
            lesson.setPinned(courseFilter != null && !isLessonInCourse(lesson, courseFilter));
        }

        // Shuffle inputs to make solver runs non-deterministic.
        if (courseFilter != null) {
            lessons = lessons.stream()
                    .filter(lesson -> isLessonInCourse(lesson, courseFilter) || isScheduled(lesson))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        Collections.shuffle(lessons);
        Collections.shuffle(timeslots);
        Collections.shuffle(rooms);

        return new Schedule(timeslots, rooms, lessons);
    }

    private boolean isLessonInCourse(Lesson lesson, Integer course) {
        return lesson.getGroup() != null && course.equals(lesson.getGroup().getCourse());
    }

    private boolean isScheduled(Lesson lesson) {
        return lesson.getTimeslot() != null && lesson.getRoom() != null;
    }

    public void saveSolution(Schedule schedule) {
        transactionTemplate.executeWithoutResult(status -> persistSolution(schedule));
    }

    private void persistSolution(Schedule schedule) {
        System.out.println("Найдено улучшение. Score: " + schedule.getScore());
        Set<Long> lessonsToUnschedule = findUnschedulableLessonIds(schedule.getLessons());
        if (!lessonsToUnschedule.isEmpty()) {
            System.out.println("Unscheduling lessons with remaining teacher conflicts or unavailable slots: " + lessonsToUnschedule.size());
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

    private Set<Long> findUnschedulableLessonIds(List<Lesson> lessons) {
        Set<Long> lessonIds = new HashSet<>();
        for (Lesson lesson : lessons) {
            if (lesson.getId() != null && hasTeacherAvailability(lesson, AvailabilityStatus.UNAVAILABLE)) {
                lessonIds.add(lesson.getId());
            }
        }

        for (int i = 0; i < lessons.size(); i++) {
            Lesson first = lessons.get(i);
            if (!canCheckTeacherConflict(first) || lessonIds.contains(first.getId())) {
                continue;
            }
            for (int j = i + 1; j < lessons.size(); j++) {
                Lesson second = lessons.get(j);
                if (!canCheckTeacherConflict(second) || lessonIds.contains(second.getId())) {
                    continue;
                }
                if (sameTeacher(first, second) && samePhysicalSlot(first, second) && weeksOverlap(first, second)) {
                    Lesson lessonToUnschedule = chooseTeacherConflictLoser(first, second);
                    lessonIds.add(lessonToUnschedule.getId());
                }
            }
        }
        return lessonIds;
    }

    private boolean canCheckTeacherConflict(Lesson lesson) {
        return lesson.getId() != null
                && lesson.getTeacher() != null
                && lesson.getTeacher().getId() != null
                && lesson.getTimeslot() != null;
    }

    private boolean sameTeacher(Lesson first, Lesson second) {
        return first.getTeacher().getId().equals(second.getTeacher().getId());
    }

    private boolean samePhysicalSlot(Lesson first, Lesson second) {
        return first.getTimeslot().getDayOfWeek() == second.getTimeslot().getDayOfWeek()
                && first.getTimeslot().getLessonNumber().equals(second.getTimeslot().getLessonNumber());
    }

    private boolean weeksOverlap(Lesson first, Lesson second) {
        Periodicity firstPeriodicity = effectivePeriodicity(first);
        Periodicity secondPeriodicity = effectivePeriodicity(second);
        return firstPeriodicity == Periodicity.WEEKLY
                || secondPeriodicity == Periodicity.WEEKLY
                || firstPeriodicity == secondPeriodicity;
    }

    private Periodicity effectivePeriodicity(Lesson lesson) {
        if (lesson.getTimeslot() != null && lesson.getTimeslot().getWeekParity() != Periodicity.WEEKLY) {
            return lesson.getTimeslot().getWeekParity();
        }
        return lesson.getPeriodicity();
    }

    private Lesson chooseTeacherConflictLoser(Lesson first, Lesson second) {
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

    private boolean hasTeacherAvailability(Lesson lesson, AvailabilityStatus status) {
        if (lesson.getTeacher() == null || lesson.getTimeslot() == null) {
            return false;
        }
        List<TeacherAvailability> availability = lesson.getTeacher().getAvailability();
        if (availability == null || availability.isEmpty()) {
            return false;
        }
        return availability.stream()
                .anyMatch(item -> item != null
                        && item.getStatus() == status
                        && item.getDayOfWeek() == lesson.getTimeslot().getDayOfWeek()
                        && lesson.getTimeslot().getLessonNumber().equals(item.getLessonNumber()));
    }

    public SolverStatus getSolverStatus() { return solverManager.getSolverStatus(SINGLETON_ID); }
    public void stopSolving() { solverManager.terminateEarly(SINGLETON_ID); }
}
