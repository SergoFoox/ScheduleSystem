package com.sergofoox.domain.solver;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.group.GroupRepository;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.plan.CoursePlan;
import com.sergofoox.domain.plan.CoursePlanRepository;
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
                Lesson l = new Lesson(plan.getSubject(), LessonType.LECTURE, plan.getTeacher(), plan.getGroup(), plan);
                l.setPeriodicity(plan.getLecturePeriodicity());
                newLessons.add(l);
            }
            for (int i = 0; i < plan.getPracticeSessionsPerWeek(); i++) {
                Lesson l = new Lesson(plan.getSubject(), LessonType.PRACTICE, plan.getTeacher(), plan.getGroup(), plan);
                l.setPeriodicity(plan.getPracticePeriodicity());
                newLessons.add(l);
            }
            for (int i = 0; i < plan.getLabSessionsPerWeek(); i++) {
                Lesson l = new Lesson(plan.getSubject(), LessonType.LABORATORY, plan.getTeacher(), plan.getGroup(), plan);
                l.setPeriodicity(plan.getLabPeriodicity());
                newLessons.add(l);
            }
        }
        System.out.println("Создано уроков: " + newLessons.size());
        lessonRepository.saveAll(newLessons);
        lessonRepository.flush();
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

    public SolverStatus getSolverStatus() { return solverManager.getSolverStatus(SINGLETON_ID); }
    public void stopSolving() { solverManager.terminateEarly(SINGLETON_ID); }
}
