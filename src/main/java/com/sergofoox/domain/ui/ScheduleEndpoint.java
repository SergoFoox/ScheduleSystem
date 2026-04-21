package com.sergofoox.domain.ui;

import com.sergofoox.domain.competence.TeacherCompetenceMatrix;
import com.sergofoox.domain.competence.TeacherCompetenceMatrixRepository;
import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.group.GroupRepository;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.plan.CoursePlan;
import com.sergofoox.domain.plan.CoursePlanRepository;
import com.sergofoox.domain.room.Room;
import com.sergofoox.domain.room.RoomRepository;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.timeslot.Timeslot;
import com.sergofoox.domain.timeslot.TimeslotRepository;
import com.sergofoox.domain.ui.dto.*;
import jakarta.annotation.security.RolesAllowed;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@BrowserCallable
@Service
public class ScheduleEndpoint {

    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final RoomRepository roomRepository;
    private final TimeslotRepository timeslotRepository;
    private final LessonRepository lessonRepository;
    private final CoursePlanRepository coursePlanRepository;
    private final TeacherCompetenceMatrixRepository teacherCompetenceMatrixRepository;

    private boolean published = false;

    public ScheduleEndpoint(
            TeacherRepository teacherRepository,
            GroupRepository groupRepository,
            RoomRepository roomRepository,
            TimeslotRepository timeslotRepository,
            LessonRepository lessonRepository,
            CoursePlanRepository coursePlanRepository,
            TeacherCompetenceMatrixRepository teacherCompetenceMatrixRepository) {
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.roomRepository = roomRepository;
        this.timeslotRepository = timeslotRepository;
        this.lessonRepository = lessonRepository;
        this.coursePlanRepository = coursePlanRepository;
        this.teacherCompetenceMatrixRepository = teacherCompetenceMatrixRepository;
    }

    @AnonymousAllowed
    public boolean isPublished() {
        return published;
    }

    @RolesAllowed({"DISPATCHER", "USER"})
    public String getCurrentUserRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "USER";
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("USER");
    }

    private boolean isDispatcher() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DISPATCHER"));
    }

    @RolesAllowed("DISPATCHER")
    public void togglePublishedStatus() {
        this.published = !this.published;
    }

    @RolesAllowed({"DISPATCHER", "USER"})
    public ScheduleGridDTO getScheduleGridData() {
        if (!published && !isDispatcher()) {
            throw new AccessDeniedException("Доступ заборонено: розклад ще в режимі чернетки. Тільки диспетчер може його бачити.");
        }

        List<Lesson> allLessons = lessonRepository.findAll();

        List<LessonDTO> lessons = allLessons.stream()
                .map(lesson -> mapToLessonDTO(lesson, allLessons))
                .toList();

        List<TeacherDTO> teachers = teacherRepository.findAll().stream()
                .map(this::mapToTeacherDTO)
                .toList();

        List<GroupDTO> groups = groupRepository.findAll().stream()
                .map(this::mapToGroupDTO)
                .toList();

        List<RoomDTO> rooms = roomRepository.findAll().stream()
                .map(this::mapToRoomDTO)
                .toList();

        List<TimeslotDTO> timeslots = timeslotRepository.findAll().stream()
                .map(this::mapToTimeslotDTO)
                .toList();

        return new ScheduleGridDTO(
                lessons,
                teachers,
                groups,
                rooms,
                timeslots
        );
    }

    @RolesAllowed({"DISPATCHER", "USER"})
    public ScheduleAnalyticsDTO getAnalytics(Long entityId, String entityType) {
        if (!published && !isDispatcher()) {
            throw new AccessDeniedException("Аналітика недоступна: розклад ще не опубліковано.");
        }

        String entityName = "Unknown";
        List<CoursePlan> plans = Collections.emptyList();
        List<Lesson> lessons = Collections.emptyList();

        if ("GROUP".equals(entityType)) {
            Group group = groupRepository.findById(entityId).orElse(null);
            if (group != null) {
                entityName = group.getName();
                plans = coursePlanRepository.findByGroup(group);
                lessons = lessonRepository.findAll().stream()
                        .filter(l -> l.getGroup() != null && l.getGroup().getId().equals(entityId))
                        .toList();
            }
        } else if ("TEACHER".equals(entityType)) {
            Teacher teacher = teacherRepository.findById(entityId).orElse(null);
            if (teacher != null) {
                entityName = teacher.getFullName();
                plans = coursePlanRepository.findByTeacher(teacher);
                lessons = lessonRepository.findAll().stream()
                        .filter(l -> l.getTeacher() != null && l.getTeacher().getId().equals(entityId))
                        .toList();
            }
        } else if ("ROOM".equals(entityType)) {
            Room room = roomRepository.findById(entityId).orElse(null);
            if (room != null) {
                entityName = room.getName();
                lessons = lessonRepository.findAll().stream()
                        .filter(l -> l.getRoom() != null && l.getRoom().getId().equals(entityId))
                        .toList();
            }
        }

        List<CourseWorkloadDTO> workloads = plans.stream()
                .map(cp -> new CourseWorkloadDTO(
                        cp.getSubject().getName(),
                        cp.getExecutedHours(),
                        cp.getTotalHours(),
                        cp.getCompletionPercentage()
                ))
                .toList();

        int windows = calculateWindows(lessons);

        return new ScheduleAnalyticsDTO(entityName, entityType, workloads, windows);
    }

    private int calculateWindows(List<Lesson> entityLessons) {
        Map<Object, List<Lesson>> byDay = entityLessons.stream()
                .filter(l -> l.getTimeslot() != null)
                .collect(Collectors.groupingBy(l -> l.getTimeslot().getDayOfWeek() + "-" + l.getTimeslot().getWeekParity()));

        int totalWindows = 0;
        for (List<Lesson> dayLessons : byDay.values()) {
            if (dayLessons.size() < 2) continue;

            List<Integer> slots = dayLessons.stream()
                    .map(l -> {
                        // This is a bit simplified, ideally timeslots have an order index
                        // We'll assume startTime can be used for ordering
                        return l.getTimeslot().getStartTime().getHour() * 60 + l.getTimeslot().getStartTime().getMinute();
                    })
                    .sorted()
                    .toList();
            
            // For now, let's just count gaps if the difference is more than usual lesson duration (e.g. 100 mins)
            for (int i = 0; i < slots.size() - 1; i++) {
                if (slots.get(i+1) - slots.get(i) > 120) { // Gap > 2 hours
                    totalWindows++;
                }
            }
        }
        return totalWindows;
    }

    @RolesAllowed("DISPATCHER")
    @Transactional
    public void moveLesson(Long lessonId, Long timeslotId, String roomName) {
        if (published) {
            throw new IllegalStateException("Редагування заборонено: розклад опубліковано");
        }
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        Timeslot timeslot = timeslotRepository.findById(timeslotId).orElseThrow();
        Room room = roomRepository.findByName(roomName).orElseThrow();

        lesson.setTimeslot(timeslot);
        lesson.setRoom(room);
        lessonRepository.save(lesson);
    }

    @RolesAllowed({"DISPATCHER", "USER"})
    public List<ReplacementCandidateDTO> getReplacementCandidates(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        if (lesson.getTimeslot() == null) {
            return Collections.emptyList();
        }

        Timeslot targetTimeslot = lesson.getTimeslot();

        List<TeacherCompetenceMatrix> matrixEntries = teacherCompetenceMatrixRepository.findBySubjectAndLessonType(
                lesson.getSubject(), lesson.getLessonType());

        List<Lesson> allLessonsAtTimeslot = lessonRepository.findByTimeslotId(targetTimeslot.getId());
        List<Long> busyTeacherIds = allLessonsAtTimeslot.stream()
                .filter(l -> l.getTeacher() != null)
                .map(l -> l.getTeacher().getId())
                .toList();

        List<Lesson> allLessons = lessonRepository.findAll();

        return matrixEntries.stream()
                .filter(entry -> !busyTeacherIds.contains(entry.getTeacher().getId()))
                .map(entry -> {
                    Teacher t = entry.getTeacher();
                    long dailyWorkload = allLessons.stream()
                            .filter(l -> l.getTeacher() != null && l.getTeacher().getId().equals(t.getId()))
                            .filter(l -> l.getTimeslot() != null &&
                                    l.getTimeslot().getDayOfWeek() == targetTimeslot.getDayOfWeek() &&
                                    l.getTimeslot().getWeekParity() == targetTimeslot.getWeekParity())
                            .count();

                    return new ReplacementCandidateDTO(
                            t.getId(),
                            t.getFullName(),
                            t.getDepartment(),
                            entry.getPriority(),
                            dailyWorkload
                    );
                })
                .sorted(Comparator.comparing(ReplacementCandidateDTO::priority)
                        .thenComparing(ReplacementCandidateDTO::currentWorkload))
                .toList();
    }

    @RolesAllowed("DISPATCHER")
    @Transactional
    public LessonDTO assignReplacement(Long lessonId, Long teacherId) {
        if (published) {
            throw new IllegalStateException("Редагування заборонено: розклад опубліковано");
        }
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        Teacher teacher = teacherRepository.findById(teacherId).orElseThrow();

        lesson.setTeacher(teacher);
        lessonRepository.save(lesson);

        List<Lesson> allLessons = lessonRepository.findAll();
        return mapToLessonDTO(lesson, allLessons);
    }

    private LessonDTO mapToLessonDTO(Lesson lesson, List<Lesson> allLessons) {
        boolean hasConflict = false;
        if (lesson.getTimeslot() != null) {
            hasConflict = allLessons.stream()
                    .filter(l -> !l.getId().equals(lesson.getId()))
                    .filter(l -> l.getTimeslot() != null && l.getTimeslot().getId().equals(lesson.getTimeslot().getId()))
                    .anyMatch(l -> 
                        (l.getTeacher() != null && lesson.getTeacher() != null && l.getTeacher().getId().equals(lesson.getTeacher().getId())) ||
                        (l.getGroup() != null && lesson.getGroup() != null && l.getGroup().getId().equals(lesson.getGroup().getId())) ||
                        (l.getRoom() != null && lesson.getRoom() != null && l.getRoom().getId().equals(lesson.getRoom().getId()))
                    );
        }

        return new LessonDTO(
                lesson.getId(),
                lesson.getSubject() != null ? lesson.getSubject().getName() : null,
                lesson.getTeacher() != null ? lesson.getTeacher().getFullName() : null,
                lesson.getGroup() != null ? lesson.getGroup().getName() : null,
                lesson.getRoom() != null ? lesson.getRoom().getName() : null,
                lesson.getTimeslot() != null ? lesson.getTimeslot().getId() : null,
                hasConflict
        );
    }

    private TeacherDTO mapToTeacherDTO(Teacher teacher) {
        return new TeacherDTO(
                teacher.getId(),
                teacher.getFullName(),
                teacher.getDepartment(),
                teacher.getPositionType()
        );
    }

    private GroupDTO mapToGroupDTO(Group group) {
        return new GroupDTO(
                group.getId(),
                group.getName(),
                group.getSize(),
                group.getCourse(),
                group.getDepartment()
        );
    }

    private RoomDTO mapToRoomDTO(Room room) {
        return new RoomDTO(
                room.getId(),
                room.getName(),
                room.getCapacity(),
                room.getBuilding(),
                room.getEquipment(),
                room.getType()
        );
    }

    private TimeslotDTO mapToTimeslotDTO(Timeslot timeslot) {
        return new TimeslotDTO(
                timeslot.getId(),
                timeslot.getDayOfWeek(),
                timeslot.getStartTime(),
                timeslot.getEndTime(),
                timeslot.getWeekParity()
        );
    }
}
