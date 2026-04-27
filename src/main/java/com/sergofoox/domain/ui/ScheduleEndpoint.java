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
import com.sergofoox.domain.solver.ScheduleService;
import com.sergofoox.domain.subject.SubjectRepository;
import com.sergofoox.domain.subject.LessonType;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.timeslot.Timeslot;
import com.sergofoox.domain.timeslot.TimeslotRepository;
import com.sergofoox.domain.ui.dto.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@BrowserCallable
@Service
@AnonymousAllowed
public class ScheduleEndpoint {

    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final RoomRepository roomRepository;
    private final SubjectRepository subjectRepository;
    private final TimeslotRepository timeslotRepository;
    private final LessonRepository lessonRepository;
    private final CoursePlanRepository coursePlanRepository;
    private final TeacherCompetenceMatrixRepository teacherCompetenceMatrixRepository;
    private final ScheduleService scheduleService;

    private boolean published = false;

    public ScheduleEndpoint(
            TeacherRepository teacherRepository,
            GroupRepository groupRepository,
            RoomRepository roomRepository,
            SubjectRepository subjectRepository,
            TimeslotRepository timeslotRepository,
            LessonRepository lessonRepository,
            CoursePlanRepository coursePlanRepository,
            TeacherCompetenceMatrixRepository teacherCompetenceMatrixRepository,
            ScheduleService scheduleService) {
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.roomRepository = roomRepository;
        this.subjectRepository = subjectRepository;
        this.timeslotRepository = timeslotRepository;
        this.lessonRepository = lessonRepository;
        this.coursePlanRepository = coursePlanRepository;
        this.teacherCompetenceMatrixRepository = teacherCompetenceMatrixRepository;
        this.scheduleService = scheduleService;
    }

    @AnonymousAllowed
    public void generateSchedule() {
        try {
            if (published) {
                throw new IllegalStateException("Неможливо згенерувати розклад: його вже опубліковано");
            }
            // 1. Спочатку генеруємо об'єкти Lesson на основі навчальних планів
            scheduleService.generateLessonsFromPlans();
            
            // 2. Запускаємо солвер для розстановки Timeslot та Room
            scheduleService.solve();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @AnonymousAllowed
    @Transactional
    public void clearSchedule() {
        try {
            if (published) {
                throw new IllegalStateException("Неможливо очистити розклад: його вже опубліковано");
            }
            List<Lesson> allLessons = lessonRepository.findAll();
            for (Lesson lesson : allLessons) {
                lesson.setTimeslot(null);
                lesson.setRoom(null);
                lessonRepository.save(lesson);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @AnonymousAllowed
    public String getSolverStatus() {
        return scheduleService.getSolverStatus().name();
    }

    @AnonymousAllowed
    public boolean isPublished() {
        return published;
    }

    @AnonymousAllowed
    public String getCurrentUserRole() {
        return "DISPATCHER"; // Simplified for testing
    }

    @AnonymousAllowed
    public void togglePublishedStatus() {
        this.published = !this.published;
    }

    @AnonymousAllowed
    public ScheduleGridDTO getScheduleGridData() {
        try {
            List<Lesson> allLessons = lessonRepository.findAll();
            List<Timeslot> allTimeslots = timeslotRepository.findAll();
            
            System.out.println("Запрос данных сетки: уроков=" + allLessons.size() + 
                               ", таймслотов=" + allTimeslots.size() + 
                               ", групп=" + groupRepository.count());

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

            List<TimeslotDTO> timeslots = allTimeslots.stream()
                    .map(this::mapToTimeslotDTO)
                    .toList();

            return new ScheduleGridDTO(lessons, teachers, groups, rooms, timeslots);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @AnonymousAllowed
    public ScheduleAnalyticsDTO getAnalytics(Long entityId, String entityType) {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private int calculateWindows(List<Lesson> entityLessons) {
        Map<Object, List<Lesson>> byDay = entityLessons.stream()
                .filter(l -> l.getTimeslot() != null)
                .collect(Collectors.groupingBy(l -> l.getTimeslot().getDayOfWeek() + "-" + l.getTimeslot().getWeekParity()));

        int totalWindows = 0;
        for (List<Lesson> dayLessons : byDay.values()) {
            if (dayLessons.size() < 2) continue;

            List<Integer> slots = dayLessons.stream()
                    .map(l -> l.getTimeslot().getStartTime().getHour() * 60 + l.getTimeslot().getStartTime().getMinute())
                    .sorted()
                    .toList();
            
            for (int i = 0; i < slots.size() - 1; i++) {
                if (slots.get(i+1) - slots.get(i) > 120) {
                    totalWindows++;
                }
            }
        }
        return totalWindows;
    }

    @AnonymousAllowed
    @Transactional
    public void moveLesson(Long lessonId, Long timeslotId, String roomName, com.sergofoox.domain.plan.Periodicity periodicity) {
        try {
            if (published) {
                throw new IllegalStateException("Редагування заборонено: розклад опубліковано");
            }
            Lesson primaryLesson = lessonRepository.findById(lessonId).orElseThrow();
            Timeslot newTimeslot = timeslotRepository.findById(timeslotId).orElseThrow();
            
            Timeslot oldTimeslot = primaryLesson.getTimeslot();
            com.sergofoox.domain.plan.Periodicity oldPeriodicity = primaryLesson.getPeriodicity();
            com.sergofoox.domain.plan.Periodicity requestedPeriodicity = periodicity != null ? periodicity : oldPeriodicity;
            com.sergofoox.domain.plan.Periodicity newPeriodicity = requestedPeriodicity;
            
            // Знаходимо всі частини цього заняття (всі підгрупи), щоб перенести їх разом
            List<Lesson> allLessons = lessonRepository.findAll();
            List<Lesson> lessonsToMove = allLessons.stream()
                    .filter(l -> shouldMoveWithPrimaryLesson(l, primaryLesson, oldTimeslot))
                    .toList();
            List<Long> movingIds = lessonsToMove.stream()
                    .map(Lesson::getId)
                    .toList();
            List<Lesson> lessonsToCombine = allLessons.stream()
                    .filter(l -> l.getId() != null && !movingIds.contains(l.getId()))
                    .filter(l -> shouldCombineAsBiWeekly(l, primaryLesson, newTimeslot, requestedPeriodicity))
                    .toList();

            if (!lessonsToCombine.isEmpty()) {
                newPeriodicity = com.sergofoox.domain.plan.Periodicity.ODD_WEEKS;
            }

            com.sergofoox.domain.plan.Periodicity swapTargetPeriodicity = newPeriodicity;
            List<Lesson> lessonsToSwap = allLessons.stream()
                    .filter(l -> l.getId() != null && !movingIds.contains(l.getId()))
                    .filter(l -> lessonsToCombine.stream().noneMatch(combine -> combine.getId().equals(l.getId())))
                    .filter(l -> shouldSwapWithMovedLesson(l, primaryLesson, newTimeslot, swapTargetPeriodicity))
                    .toList();

            for (Lesson lesson : lessonsToMove) {
                lesson.setTimeslot(newTimeslot);
                lesson.setPeriodicity(newPeriodicity);
                // Якщо кімната вказана явно - змінюємо, інакше залишаємо поточну
                if (roomName != null && !roomName.isBlank()) {
                    Room room = roomRepository.findByName(roomName).orElse(null);
                    lesson.setRoom(room);
                }
                lessonRepository.save(lesson);
            }
            for (Lesson lesson : lessonsToSwap) {
                lesson.setTimeslot(oldTimeslot);
                lesson.setPeriodicity(oldPeriodicity);
                lessonRepository.save(lesson);
            }
            for (Lesson lesson : lessonsToCombine) {
                lesson.setTimeslot(newTimeslot);
                lesson.setPeriodicity(com.sergofoox.domain.plan.Periodicity.EVEN_WEEKS);
                lessonRepository.save(lesson);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private boolean shouldCombineAsBiWeekly(
            Lesson candidate,
            Lesson movedLesson,
            Timeslot targetTimeslot,
            com.sergofoox.domain.plan.Periodicity requestedPeriodicity) {
        if (requestedPeriodicity != com.sergofoox.domain.plan.Periodicity.WEEKLY) {
            return false;
        }
        if (candidate.getTimeslot() == null || targetTimeslot == null || movedLesson.getGroup() == null || candidate.getGroup() == null) {
            return false;
        }
        if (!candidate.getGroup().getId().equals(movedLesson.getGroup().getId())) {
            return false;
        }
        if (candidate.getTimeslot().getDayOfWeek() != targetTimeslot.getDayOfWeek()
                || !candidate.getTimeslot().getLessonNumber().equals(targetTimeslot.getLessonNumber())) {
            return false;
        }
        if (candidate.getSubgroup() != null && candidate.getSubgroup() > 0) {
            return false;
        }
        return effectivePeriodicity(candidate) == com.sergofoox.domain.plan.Periodicity.WEEKLY;
    }

    private boolean shouldSwapWithMovedLesson(
            Lesson candidate,
            Lesson movedLesson,
            Timeslot targetTimeslot,
            com.sergofoox.domain.plan.Periodicity targetPeriodicity) {
        if (candidate.getTimeslot() == null || targetTimeslot == null || movedLesson.getGroup() == null || candidate.getGroup() == null) {
            return false;
        }
        if (!candidate.getGroup().getId().equals(movedLesson.getGroup().getId())) {
            return false;
        }
        if (candidate.getTimeslot().getDayOfWeek() != targetTimeslot.getDayOfWeek()
                || !candidate.getTimeslot().getLessonNumber().equals(targetTimeslot.getLessonNumber())) {
            return false;
        }
        if (candidate.getSubgroup() != null && candidate.getSubgroup() > 0
                && movedLesson.getSubgroup() != null && movedLesson.getSubgroup() > 0
                && !candidate.getSubgroup().equals(movedLesson.getSubgroup())) {
            return false;
        }
        return effectivePeriodicity(candidate) == targetPeriodicity;
    }

    private boolean shouldMoveWithPrimaryLesson(Lesson candidate, Lesson primaryLesson, Timeslot oldTimeslot) {
        if (candidate.getId() != null && candidate.getId().equals(primaryLesson.getId())) {
            return true;
        }
        if (oldTimeslot == null || candidate.getTimeslot() == null || !candidate.getTimeslot().getId().equals(oldTimeslot.getId())) {
            return false;
        }
        if (primaryLesson.getSubgroup() == null || primaryLesson.getSubgroup() == 0
                || primaryLesson.getSplitGroupIndex() == null || primaryLesson.getSplitGroupIndex() == 0) {
            return false;
        }
        return candidate.getCoursePlan() != null && primaryLesson.getCoursePlan() != null
                && candidate.getCoursePlan().getId().equals(primaryLesson.getCoursePlan().getId())
                && candidate.getLessonType() == primaryLesson.getLessonType()
                && candidate.getSplitGroupIndex() != null
                && candidate.getSplitGroupIndex().equals(primaryLesson.getSplitGroupIndex())
                && candidate.getSubgroup() != null
                && candidate.getSubgroup() > 0;
    }

    @AnonymousAllowed
    public List<ReplacementCandidateDTO> getReplacementCandidates(Long lessonId) {
        try {
            Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
            if (lesson.getTimeslot() == null) return Collections.emptyList();

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
                                t.getId(), t.getFullName(), t.getDepartment(), entry.getPriority(), dailyWorkload
                        );
                    })
                    .sorted(Comparator.comparing(ReplacementCandidateDTO::priority)
                            .thenComparing(ReplacementCandidateDTO::currentWorkload))
                    .toList();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @AnonymousAllowed
    @Transactional
    public void unassignLesson(Long lessonId) {
        try {
            if (published) throw new IllegalStateException("Редагування заборонено: розклад опубліковано");
            Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
            lesson.setTimeslot(null);
            lesson.setRoom(null);
            lessonRepository.save(lesson);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @AnonymousAllowed
    @Transactional
    public LessonDTO assignReplacement(Long lessonId, Long teacherId, Long roomId, Long subjectId) {
        try {
            if (published) throw new IllegalStateException("Редагування заборонено: розклад опубліковано");
            Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
            
            if (subjectId != null) {
                Subject subject = subjectRepository.findById(subjectId).orElseThrow();
                lesson.setSubject(subject);
                
                // Обов'язково оновлюємо навчальний план, бо Lesson прив'язаний до нього
                CoursePlan plan = coursePlanRepository.findByGroup(lesson.getGroup()).stream()
                        .filter(p -> p.getSubject().getId().equals(subjectId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Ця дисципліна не входить до плану групи"));
                lesson.setCoursePlan(plan);
            }

            if (teacherId != null) {
                Teacher teacher = teacherRepository.findById(teacherId).orElseThrow();
                lesson.setTeacher(teacher);
            }

            if (roomId != null) {
                Room room = roomRepository.findById(roomId).orElse(null);
                lesson.setRoom(room);
            }
            
            lessonRepository.save(lesson);

            List<Lesson> allLessons = lessonRepository.findAll();
            return mapToLessonDTO(lesson, allLessons);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @AnonymousAllowed
    @Transactional
    public void assignManualLesson(Long groupId, Long subjectId, Long timeslotId, Long roomId, Long teacherId, Integer subgroup, com.sergofoox.domain.plan.Periodicity periodicity) {
        try {
            if (published) throw new IllegalStateException("Розклад опубліковано");
            
            Group group = groupRepository.findById(groupId).orElseThrow();
            Subject subject = subjectRepository.findById(subjectId).orElseThrow();
            Timeslot timeslot = timeslotRepository.findById(timeslotId).orElseThrow();
            
            CoursePlan plan = coursePlanRepository.findByGroup(group).stream()
                    .filter(p -> p.getSubject().getId().equals(subjectId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Ця дисципліна не входить до плану групи"));

            Lesson lesson = new Lesson();
            lesson.setGroup(group);
            lesson.setSubject(subject);
            lesson.setTimeslot(timeslot);
            lesson.setCoursePlan(plan);
            lesson.setLessonType(LessonType.LECTURE);
            lesson.setSubgroup(subgroup != null ? subgroup : 0);
            lesson.setPeriodicity(periodicity != null ? periodicity : com.sergofoox.domain.plan.Periodicity.WEEKLY);
            
            if (teacherId != null) {
                Teacher teacher = teacherRepository.findById(teacherId).orElseThrow();
                lesson.setTeacher(teacher);
            }
            
            if (roomId != null) {
                Room room = roomRepository.findById(roomId).orElse(null);
                lesson.setRoom(room);
            }
            
            lessonRepository.save(lesson);
        } catch (Exception e) {
            System.err.println("Error in assignManualLesson: " + e.getMessage());
            throw e;
        }
    }

    private LessonDTO mapToLessonDTO(Lesson lesson, List<Lesson> allLessons) {
        boolean hasConflict = false;
        if (lesson.getTimeslot() != null && lesson.getId() != null) {
            hasConflict = allLessons.stream()
                    .filter(l -> l.getId() != null && !l.getId().equals(lesson.getId()))
                    .filter(l -> samePhysicalSlot(l, lesson))
                    .anyMatch(l -> {
                        if (!weeksOverlap(l, lesson)) return false;

                        // 1. Конфлікт викладача
                        boolean teacherConflict = l.getTeacher() != null && lesson.getTeacher() != null && 
                                                 l.getTeacher().getId().equals(lesson.getTeacher().getId());
                        
                        // 2. Конфлікт аудиторії
                        boolean roomConflict = l.getRoom() != null && lesson.getRoom() != null && 
                                              l.getRoom().getId().equals(lesson.getRoom().getId());
                        boolean subjectConflict = l.getSubject() != null && lesson.getSubject() != null
                                && l.getSubject().getId().equals(lesson.getSubject().getId())
                                && !sameSplitGroupLesson(l, lesson);
                        
                        // 3. Конфлікт групи
                        boolean groupConflict = false;
                        if (l.getGroup() != null && lesson.getGroup() != null && l.getGroup().getId().equals(lesson.getGroup().getId())) {
                            int s1 = l.getSubgroup() != null ? l.getSubgroup() : 0;
                            int s2 = lesson.getSubgroup() != null ? lesson.getSubgroup() : 0;
                            if (s1 == 0 || s2 == 0 || s1 == s2) {
                                groupConflict = true;
                            }
                        }
                        
                        return teacherConflict || roomConflict || subjectConflict || groupConflict;
                    });
        }

        return new LessonDTO(
                lesson.getId(),
                lesson.getSubject() != null ? lesson.getSubject().getName() : null,
                lesson.getTeacher() != null ? lesson.getTeacher().getFullName() : null,
                lesson.getGroup() != null ? lesson.getGroup().getName() : null,
                lesson.getRoom() != null ? lesson.getRoom().getName() : null,
                lesson.getTimeslot() != null ? lesson.getTimeslot().getId() : null,
                hasConflict,
                lesson.getSubgroup(),
                lesson.getGroup() != null ? lesson.getGroup().getId() : null,
                lesson.getSubject() != null ? lesson.getSubject().getId() : null,
                lesson.getRoom() != null ? lesson.getRoom().getId() : null,
                lesson.getTeacher() != null ? lesson.getTeacher().getId() : null,
                lesson.getPeriodicity()
        );
    }

    private boolean samePhysicalSlot(Lesson first, Lesson second) {
        if (first.getTimeslot() == null || second.getTimeslot() == null) return false;
        return first.getTimeslot().getDayOfWeek() == second.getTimeslot().getDayOfWeek()
                && first.getTimeslot().getLessonNumber().equals(second.getTimeslot().getLessonNumber());
    }

    private boolean weeksOverlap(Lesson first, Lesson second) {
        com.sergofoox.domain.plan.Periodicity firstPeriodicity = effectivePeriodicity(first);
        com.sergofoox.domain.plan.Periodicity secondPeriodicity = effectivePeriodicity(second);
        return firstPeriodicity == com.sergofoox.domain.plan.Periodicity.WEEKLY
                || secondPeriodicity == com.sergofoox.domain.plan.Periodicity.WEEKLY
                || firstPeriodicity == secondPeriodicity;
    }

    private boolean sameSplitGroupLesson(Lesson first, Lesson second) {
        if (first.getGroup() == null || second.getGroup() == null
                || first.getCoursePlan() == null || second.getCoursePlan() == null
                || first.getSubgroup() == null || second.getSubgroup() == null
                || first.getSplitGroupIndex() == null || second.getSplitGroupIndex() == null) {
            return false;
        }
        return first.getGroup().getId().equals(second.getGroup().getId())
                && first.getCoursePlan().getId().equals(second.getCoursePlan().getId())
                && first.getLessonType() == second.getLessonType()
                && first.getSplitGroupIndex().equals(second.getSplitGroupIndex())
                && first.getSubgroup() > 0
                && second.getSubgroup() > 0
                && !first.getSubgroup().equals(second.getSubgroup());
    }

    private com.sergofoox.domain.plan.Periodicity effectivePeriodicity(Lesson lesson) {
        if (lesson.getTimeslot() != null
                && lesson.getTimeslot().getWeekParity() != com.sergofoox.domain.plan.Periodicity.WEEKLY) {
            return lesson.getTimeslot().getWeekParity();
        }
        return lesson.getPeriodicity();
    }

    private TeacherDTO mapToTeacherDTO(Teacher teacher) {
        return new TeacherDTO(
                teacher.getId(),
                teacher.getFullName(),
                teacher.getDepartment(),
                teacher.getSpecialization(),
                teacher.getPositionType(),
                teacher.getWeeklyHourLimit(),
                teacher.getMaxWorkingDaysPerWeek(),
                teacher.getAssignedRoom() != null ? teacher.getAssignedRoom().getId() : null,
                teacher.getAssignedRoom() != null ? teacher.getAssignedRoom().getName() : null
        );
    }

    private GroupDTO mapToGroupDTO(Group group) {
        String curatorName = null;
        if (group.getCuratorId() != null) {
            curatorName = teacherRepository.findById(group.getCuratorId())
                    .map(Teacher::getFullName)
                    .orElse(null);
        }
        return new GroupDTO(
                group.getId(),
                group.getName(),
                group.getSize(),
                group.getCourse(),
                group.getDepartment(),
                group.getCuratorId(),
                curatorName
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
                timeslot.getWeekParity(),
                timeslot.getLessonNumber()
        );
    }
}
