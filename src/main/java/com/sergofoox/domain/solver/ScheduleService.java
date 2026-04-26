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
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.timeslot.Timeslot;
import com.sergofoox.domain.timeslot.TimeslotRepository;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ScheduleService(SolverManager<Schedule, UUID> solverManager,
                           TeacherRepository teacherRepository,
                           GroupRepository groupRepository,
                           RoomRepository roomRepository,
                           TimeslotRepository timeslotRepository,
                           LessonRepository lessonRepository,
                           CoursePlanRepository coursePlanRepository,
                           SubjectRepository subjectRepository) {
        this.solverManager = solverManager;
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.roomRepository = roomRepository;
        this.timeslotRepository = timeslotRepository;
        this.lessonRepository = lessonRepository;
        this.coursePlanRepository = coursePlanRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public void generateLessonsFromPlans() {
        System.out.println("=== ГЕНЕРАЦИЯ УРОКОВ ===");
        lessonRepository.deleteAll();
        lessonRepository.flush();
        
        List<CoursePlan> allPlans = coursePlanRepository.findAll();
        List<Lesson> newLessons = new ArrayList<>();

        for (CoursePlan plan : allPlans) {
            if (plan.getTeacher() == null) continue;

            // Генерируем уроки согласно часам и периодичности из плана
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
        System.out.println("Создано уроков: " + newLessons.size());
        lessonRepository.saveAll(newLessons);
        lessonRepository.flush();
    }

    private void addLessonsForPlan(List<Lesson> lessons, CoursePlan plan, LessonType lessonType, com.sergofoox.domain.plan.Periodicity periodicity, int splitGroupIndex) {
        if (plan.getSecondTeacher() != null) {
            Lesson firstSubgroup = new Lesson(plan.getSubject(), lessonType, plan.getTeacher(), plan.getGroup(), plan, 1);
            firstSubgroup.setPeriodicity(periodicity);
            firstSubgroup.setSplitGroupIndex(splitGroupIndex);
            lessons.add(firstSubgroup);

            Lesson secondSubgroup = new Lesson(plan.getSubject(), lessonType, plan.getSecondTeacher(), plan.getGroup(), plan, 2);
            secondSubgroup.setPeriodicity(periodicity);
            secondSubgroup.setSplitGroupIndex(splitGroupIndex);
            lessons.add(secondSubgroup);
            return;
        }

        Lesson lesson = new Lesson(plan.getSubject(), lessonType, plan.getTeacher(), plan.getGroup(), plan);
        lesson.setPeriodicity(periodicity);
        lesson.setSplitGroupIndex(splitGroupIndex);
        lessons.add(lesson);
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
        if (solverManager.getSolverStatus(SINGLETON_ID) != SolverStatus.NOT_SOLVING) {
            solverManager.terminateEarly(SINGLETON_ID);
        }
        solverManager.solveAndListen(SINGLETON_ID, this::findById, this::saveSolution);
    }

    public Schedule findById(UUID id) {
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

        // Принудительная прошивка ссылок
        for (Lesson lesson : lessons) {
            if (lesson.getRoom() != null) lesson.setRoom(roomMap.get(lesson.getRoom().getId()));
            if (lesson.getTimeslot() != null) lesson.setTimeslot(timeslotMap.get(lesson.getTimeslot().getId()));
            lesson.setTeacher(teacherMap.get(lesson.getTeacher().getId()));
            lesson.setGroup(groupMap.get(lesson.getGroup().getId()));
            lesson.setSubject(subjectMap.get(lesson.getSubject().getId()));
            lesson.setCoursePlan(planMap.get(lesson.getCoursePlan().getId()));
        }

        // Перемешиваем для рандома
        Collections.shuffle(lessons);
        Collections.shuffle(timeslots);
        Collections.shuffle(rooms);

        return new Schedule(timeslots, rooms, lessons);
    }

    @Transactional
    public void saveSolution(Schedule schedule) {
        syncSplitSubgroupLessons(schedule);
        System.out.println("Найдено улучшение. Score: " + schedule.getScore());
        for (Lesson lesson : schedule.getLessons()) {
            if (lesson.getId() != null) {
                lessonRepository.findById(lesson.getId()).ifPresent(dbLesson -> {
                    dbLesson.setTimeslot(lesson.getTimeslot());
                    dbLesson.setRoom(lesson.getRoom());
                    lessonRepository.save(dbLesson);
                });
            }
        }
        lessonRepository.flush();
    }

    private void syncSplitSubgroupLessons(Schedule schedule) {
        Map<String, List<Lesson>> splitGroups = schedule.getLessons().stream()
                .filter(lesson -> lesson.getSubgroup() != null && lesson.getSubgroup() > 0)
                .filter(lesson -> lesson.getSplitGroupIndex() != null && lesson.getSplitGroupIndex() > 0)
                .collect(Collectors.groupingBy(lesson -> lesson.getCoursePlan().getId()
                        + "|" + lesson.getLessonType()
                        + "|" + lesson.getPeriodicity()
                        + "|" + lesson.getSplitGroupIndex()));

        for (List<Lesson> splitLessons : splitGroups.values()) {
            Lesson first = splitLessons.stream()
                    .filter(lesson -> lesson.getSubgroup() == 1)
                    .findFirst()
                    .orElse(null);
            Lesson second = splitLessons.stream()
                    .filter(lesson -> lesson.getSubgroup() == 2)
                    .findFirst()
                    .orElse(null);

            if (first == null || second == null || first.getTimeslot() == null) {
                continue;
            }

            second.setTimeslot(first.getTimeslot());
            if (second.getRoom() == null || second.getRoom().equals(first.getRoom()) || isRoomBusy(schedule, second, second.getRoom())) {
                findAvailableRoom(schedule, second, first.getRoom()).ifPresent(second::setRoom);
            }
        }
    }

    private Optional<Room> findAvailableRoom(Schedule schedule, Lesson lesson, Room excludedRoom) {
        return schedule.getRooms().stream()
                .filter(room -> excludedRoom == null || !room.equals(excludedRoom))
                .filter(room -> !isRoomBusy(schedule, lesson, room))
                .findFirst();
    }

    private boolean isRoomBusy(Schedule schedule, Lesson targetLesson, Room room) {
        if (room == null || targetLesson.getTimeslot() == null) return false;
        return schedule.getLessons().stream()
                .filter(lesson -> lesson != targetLesson)
                .filter(lesson -> lesson.getRoom() != null && lesson.getTimeslot() != null)
                .filter(lesson -> room.equals(lesson.getRoom()))
                .filter(lesson -> samePhysicalSlot(lesson, targetLesson))
                .anyMatch(lesson -> weeksOverlap(lesson, targetLesson));
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

    public SolverStatus getSolverStatus() { return solverManager.getSolverStatus(SINGLETON_ID); }
    public void stopSolving() { solverManager.terminateEarly(SINGLETON_ID); }
}
