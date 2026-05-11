package com.sergofoox.domain.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergofoox.domain.competence.Priority;
import com.sergofoox.domain.competence.TeacherCompetenceMatrix;
import com.sergofoox.domain.competence.TeacherCompetenceMatrixRepository;
import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.group.GroupRepository;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.plan.CoursePlan;
import com.sergofoox.domain.plan.CoursePlanRepository;
import com.sergofoox.domain.plan.Periodicity;
import com.sergofoox.domain.plan.RoomType;
import com.sergofoox.domain.room.Room;
import com.sergofoox.domain.room.RoomRepository;
import com.sergofoox.domain.saved.SavedSchedule;
import com.sergofoox.domain.saved.SavedScheduleLesson;
import com.sergofoox.domain.saved.SavedScheduleRepository;
import com.sergofoox.domain.solver.ScheduleService;
import com.sergofoox.domain.subject.SubjectRepository;
import com.sergofoox.domain.subject.LessonType;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.teacher.PositionType;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.timeslot.Timeslot;
import com.sergofoox.domain.timeslot.TimeslotRepository;
import com.sergofoox.domain.ui.dto.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@BrowserCallable
@Service
@AnonymousAllowed
public class ScheduleEndpoint {

    private static final DateTimeFormatter SAVED_SCHEDULE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final long BUILT_IN_TEMPLATE_ID = -1L;
    private static final String BUILT_IN_TEMPLATE_NAME = "Базовий шаблон";
    private static final String LEGACY_BUILT_IN_TEMPLATE_NAME = "Базовий шаблон (імпортовано)";
    private static final int BUILT_IN_TEMPLATE_LESSON_COUNT = 185;

    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final RoomRepository roomRepository;
    private final SubjectRepository subjectRepository;
    private final TimeslotRepository timeslotRepository;
    private final LessonRepository lessonRepository;
    private final CoursePlanRepository coursePlanRepository;
    private final TeacherCompetenceMatrixRepository teacherCompetenceMatrixRepository;
    private final ScheduleService scheduleService;
    private final SavedScheduleRepository savedScheduleRepository;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final TemplateAccessService templateAccessService;

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
            ScheduleService scheduleService,
            SavedScheduleRepository savedScheduleRepository,
            DataSource dataSource,
            TemplateAccessService templateAccessService,
            ObjectMapper objectMapper) {
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.roomRepository = roomRepository;
        this.subjectRepository = subjectRepository;
        this.timeslotRepository = timeslotRepository;
        this.lessonRepository = lessonRepository;
        this.coursePlanRepository = coursePlanRepository;
        this.teacherCompetenceMatrixRepository = teacherCompetenceMatrixRepository;
        this.scheduleService = scheduleService;
        this.savedScheduleRepository = savedScheduleRepository;
        this.dataSource = dataSource;
        this.templateAccessService = templateAccessService;
        this.objectMapper = objectMapper;
    }

    @AnonymousAllowed
    public void generateSchedule() {
        generateScheduleForCourse(0);
    }

    @AnonymousAllowed
    public void generateScheduleForCourse(Integer course) {
        try {
            if (published) {
                throw new IllegalStateException("Неможливо згенерувати розклад: його вже опубліковано");
            }
            Integer courseFilter = course != null && course > 0 ? course : null;
            if (courseFilter != null && courseFilter > 4) {
                throw new IllegalArgumentException("Курс має бути від 1 до 4");
            }
            scheduleService.generateLessonsFromPlans(courseFilter);
            scheduleService.solve(courseFilter);
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
            templateAccessService.requireWritableTemplate();
            clearLessonPlacements();
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
    @Transactional(readOnly = true)
    public List<SavedScheduleDTO> getSavedSchedules() {
        try {
            List<SavedScheduleDTO> savedSchedules = new ArrayList<>(savedScheduleRepository.findAllByOrderBySortOrderAscUpdatedAtDescIdAsc().stream()
                    .filter(savedSchedule -> !isBuiltInTemplateName(savedSchedule.getName()))
                    .map(this::mapToSavedScheduleDTO)
                    .toList());
            savedSchedules.add(0, new SavedScheduleDTO(
                    BUILT_IN_TEMPLATE_ID,
                    BUILT_IN_TEMPLATE_NAME,
                    "",
                    "",
                    getBuiltInTemplateLessonCount(),
                    true,
                    true,
                    false));
            return savedSchedules;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @AnonymousAllowed
    @Transactional
    public void toggleAutosave(Long id, boolean enabled) {
        if (id == null || id < 0) return;
        savedScheduleRepository.findById(id).ifPresent(s -> {
            s.setAutosaveEnabled(enabled);
            savedScheduleRepository.save(s);
        });
    }

    @AnonymousAllowed
    @Transactional
    public SavedScheduleDTO saveCurrentSchedule(String name) {
        String normalizedName = normalizeSavedScheduleName(name);
        if (isBuiltInTemplateName(normalizedName) || savedScheduleRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
            throw new IllegalArgumentException("Шаблон із такою назвою вже існує");
        }

        LocalDateTime now = LocalDateTime.now();
        SavedSchedule savedSchedule = new SavedSchedule();
        savedSchedule.setName(normalizedName);
        savedSchedule.setCreatedAt(now);
        savedSchedule.setUpdatedAt(now);
        savedSchedule.setSortOrder(nextSavedScheduleSortOrder());
        savedSchedule.setFullTemplate(true);
        savedSchedule.setSnapshotJson(serializeSnapshot(clearSnapshotLessonPlacements(captureCurrentSnapshot())));
        savedSchedule.replaceLessons(List.of());

        return mapToSavedScheduleDTO(savedScheduleRepository.save(savedSchedule));
    }

    @AnonymousAllowed
    @Transactional
    public SavedScheduleDTO saveCurrentScheduleToSavedSchedule(Long id) {
        templateAccessService.requireWritableTemplate();
        if (id == null || id < 0) {
            throw new IllegalArgumentException("Базовий шаблон не можна перезаписати");
        }

        SavedSchedule savedSchedule = savedScheduleRepository.findById(id).orElseThrow();
        savedSchedule.setUpdatedAt(LocalDateTime.now());
        if (savedSchedule.isFullTemplate()) {
            savedSchedule.setSnapshotJson(serializeSnapshot(captureCurrentSnapshot()));
        }
        savedSchedule.replaceLessons(lessonRepository.findAll().stream()
                .map(this::createSavedScheduleLesson)
                .toList());

        return mapToSavedScheduleDTO(savedScheduleRepository.save(savedSchedule));
    }

    @AnonymousAllowed
    @Transactional
    public SavedScheduleDTO copyBuiltInTemplate(String name) {
        String normalizedName = normalizeSavedScheduleName(name);
        if (isBuiltInTemplateName(normalizedName) || savedScheduleRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
            throw new IllegalArgumentException("Шаблон із такою назвою вже існує");
        }

        if (!templateAccessService.isBaseTemplateLocked()) {
            importBuiltInTemplate();
        }

        LocalDateTime now = LocalDateTime.now();
        SavedSchedule savedSchedule = new SavedSchedule();
        savedSchedule.setName(normalizedName);
        savedSchedule.setCreatedAt(now);
        savedSchedule.setUpdatedAt(now);
        savedSchedule.setSortOrder(nextSavedScheduleSortOrder());
        savedSchedule.setFullTemplate(true);
        savedSchedule.setSnapshotJson(serializeSnapshot(captureCurrentSnapshot()));
        savedSchedule.replaceLessons(lessonRepository.findAll().stream()
                .map(this::createSavedScheduleLesson)
                .toList());

        SavedSchedule saved = savedScheduleRepository.save(savedSchedule);
        templateAccessService.activateEditableTemplate(saved.getId());
        return mapToSavedScheduleDTO(saved);
    }

    @AnonymousAllowed
    @Transactional
    public SavedScheduleDTO copySavedSchedule(Long id, String name) {
        if (id == null || id < 0) {
            throw new IllegalArgumentException("Базовий шаблон копіюється окремою дією");
        }

        String normalizedName = normalizeSavedScheduleName(name);
        if (isBuiltInTemplateName(normalizedName) || savedScheduleRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
            throw new IllegalArgumentException("Шаблон із такою назвою вже існує");
        }

        SavedSchedule source = savedScheduleRepository.findById(id).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        SavedSchedule copy = new SavedSchedule();
        copy.setName(normalizedName);
        copy.setCreatedAt(now);
        copy.setUpdatedAt(now);
        copy.setSortOrder(nextSavedScheduleSortOrder());
        copy.setFullTemplate(source.isFullTemplate());
        copy.setSnapshotJson(source.getSnapshotJson());
        copy.replaceLessons(source.getLessons().stream()
                .map(this::copySavedScheduleLesson)
                .toList());

        return mapToSavedScheduleDTO(savedScheduleRepository.save(copy));
    }

    @AnonymousAllowed
    @Transactional
    public void loadSavedSchedule(Long id) {
        if (published) {
            throw new IllegalStateException("Неможливо завантажити збережений розклад: розклад вже опубліковано");
        }

        if (Objects.equals(id, BUILT_IN_TEMPLATE_ID)) {
            importBuiltInTemplate();
            templateAccessService.lockBaseTemplate();
            return;
        }

        SavedSchedule savedSchedule = savedScheduleRepository.findById(id).orElseThrow();
        if (savedSchedule.isFullTemplate() && savedSchedule.getSnapshotJson() != null && !savedSchedule.getSnapshotJson().isBlank()) {
            restoreSnapshot(deserializeSnapshot(savedSchedule.getSnapshotJson()));
            templateAccessService.activateEditableTemplate(savedSchedule.getId());
            return;
        }

        List<Lesson> lessons = lessonRepository.findAll();
        Map<Long, Lesson> lessonsById = lessons.stream()
                .filter(lesson -> lesson.getId() != null)
                .collect(Collectors.toMap(Lesson::getId, lesson -> lesson));
        Set<Long> restoredLessonIds = new HashSet<>();

        for (SavedScheduleLesson savedLesson : savedSchedule.getLessons()) {
            resolveSavedLesson(savedLesson, lessonsById, lessons, restoredLessonIds)
                    .ifPresent(lesson -> {
                        applySavedLesson(lesson, savedLesson);
                        restoredLessonIds.add(lesson.getId());
                        lessonRepository.save(lesson);
                    });
        }

        for (Lesson lesson : lessons) {
            if (lesson.getId() != null && !restoredLessonIds.contains(lesson.getId())) {
                lesson.setTimeslot(null);
                lesson.setRoom(null);
                lessonRepository.save(lesson);
            }
        }
        lessonRepository.flush();
        templateAccessService.activateEditableTemplate(savedSchedule.getId());
    }

    @AnonymousAllowed
    @Transactional
    public void deleteSavedSchedule(Long id) {
        if (id == null || id < 0) {
            return;
        }
        boolean deletedScheduleIsActive = Objects.equals(templateAccessService.getActiveSavedScheduleId(), id);
        savedScheduleRepository.deleteById(id);
        if (deletedScheduleIsActive) {
            clearWorkingData();
            published = false;
            templateAccessService.resetBaseTemplateSession();
        }
    }

    @AnonymousAllowed
    @Transactional
    public SavedScheduleDTO renameSavedSchedule(Long id, String name) {
        if (id == null || id < 0) {
            throw new IllegalArgumentException("Базовий шаблон не можна перейменувати");
        }

        String normalizedName = normalizeSavedScheduleName(name);
        if (isBuiltInTemplateName(normalizedName)) {
            throw new IllegalArgumentException("Цю назву зарезервовано для базового шаблону");
        }

        SavedSchedule savedSchedule = savedScheduleRepository.findById(id).orElseThrow();
        Optional<SavedSchedule> duplicate = savedScheduleRepository.findByNameIgnoreCase(normalizedName);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw new IllegalArgumentException("Шаблон із такою назвою вже існує");
        }

        savedSchedule.setName(normalizedName);
        savedSchedule.setUpdatedAt(LocalDateTime.now());
        return mapToSavedScheduleDTO(savedScheduleRepository.save(savedSchedule));
    }

    @AnonymousAllowed
    @Transactional
    public void reorderSavedSchedules(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<SavedSchedule> savedSchedules = savedScheduleRepository.findAllByOrderBySortOrderAscUpdatedAtDescIdAsc().stream()
                .filter(savedSchedule -> !isBuiltInTemplateName(savedSchedule.getName()))
                .toList();
        Map<Long, SavedSchedule> savedSchedulesById = savedSchedules.stream()
                .filter(savedSchedule -> savedSchedule.getId() != null)
                .collect(Collectors.toMap(SavedSchedule::getId, savedSchedule -> savedSchedule));

        List<Long> orderedIds = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .filter(savedSchedulesById::containsKey)
                .collect(Collectors.toCollection(ArrayList::new));

        savedSchedules.stream()
                .map(SavedSchedule::getId)
                .filter(id -> !orderedIds.contains(id))
                .forEach(orderedIds::add);

        for (int index = 0; index < orderedIds.size(); index++) {
            savedSchedulesById.get(orderedIds.get(index)).setSortOrder(index);
        }
        savedScheduleRepository.saveAll(savedSchedulesById.values());
    }

    @AnonymousAllowed
    public Long getActiveSavedScheduleId() {
        return templateAccessService.getActiveSavedScheduleId();
    }

    @AnonymousAllowed
    public boolean isPublished() {
        return published;
    }

    @AnonymousAllowed
    public boolean isBaseTemplateLocked() {
        return templateAccessService.isBaseTemplateLocked();
    }

    @AnonymousAllowed
    @Transactional
    public void resetBaseTemplateOnPageReload() {
        if (!templateAccessService.isBaseTemplateOpened()) {
            return;
        }
        clearWorkingData();
        published = false;
        templateAccessService.resetBaseTemplateSession();
    }

    @AnonymousAllowed
    public String getCurrentUserRole() {
        return "DISPATCHER";
    }

    @AnonymousAllowed
    public void togglePublishedStatus() {
        templateAccessService.requireWritableTemplate();
        this.published = !this.published;
    }

    @AnonymousAllowed
    @Transactional(readOnly = true)
    public ScheduleGridDTO getScheduleGridData() {
        try {
            System.out.println(">>> Завантаження даних для сітки...");
            List<Lesson> allLessons = lessonRepository.findAll();
            List<Timeslot> allTimeslots = timeslotRepository.findAll();
            List<Teacher> allTeachers = teacherRepository.findAll();
            List<Group> allGroups = groupRepository.findAll();
            List<Room> allRooms = roomRepository.findAll();
            
            Map<Long, Teacher> teachersById = allTeachers.stream()
                    .filter(t -> t.getId() != null)
                    .collect(Collectors.toMap(Teacher::getId, Function.identity(), (a, b) -> a));
            
            Map<Long, List<Lesson>> lessonsByTimeslot = allLessons.stream()
                    .filter(l -> l.getTimeslot() != null && l.getTimeslot().getId() != null)
                    .collect(Collectors.groupingBy(l -> l.getTimeslot().getId()));

            List<LessonDTO> lessons = allLessons.stream()
                    .map(lesson -> {
                        List<Lesson> sameSlot = (lesson.getTimeslot() != null && lesson.getTimeslot().getId() != null)
                            ? lessonsByTimeslot.getOrDefault(lesson.getTimeslot().getId(), Collections.emptyList())
                            : Collections.emptyList();
                        return mapToLessonDTO(lesson, sameSlot);
                    })
                    .toList();

            List<TeacherDTO> teachers = allTeachers.stream()
                    .map(this::mapToTeacherDTO)
                    .toList();

            List<GroupDTO> groups = allGroups.stream()
                    .map(g -> mapToGroupDTO(g, teachersById))
                    .toList();

            List<RoomDTO> rooms = allRooms.stream()
                    .map(this::mapToRoomDTO)
                    .toList();

            List<TimeslotDTO> timeslots = allTimeslots.stream()
                    .map(this::mapToTimeslotDTO)
                    .toList();

            return new ScheduleGridDTO(lessons, teachers, groups, rooms, timeslots);
        } catch (Exception e) {
            System.err.println("!!! Помилка в getScheduleGridData: " + e.getMessage());
            e.printStackTrace();
            return new ScheduleGridDTO(Collections.emptyList(), Collections.emptyList(), 
                                     Collections.emptyList(), Collections.emptyList(), 
                                     Collections.emptyList());
        }
    }

    @AnonymousAllowed
    @Transactional(readOnly = true)
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

    private SavedScheduleLesson createSavedScheduleLesson(Lesson lesson) {
        SavedScheduleLesson savedLesson = new SavedScheduleLesson();
        savedLesson.setLessonId(lesson.getId());
        savedLesson.setCoursePlanId(idOf(lesson.getCoursePlan()));
        savedLesson.setGroupId(idOf(lesson.getGroup()));
        savedLesson.setSubjectId(idOf(lesson.getSubject()));
        savedLesson.setTeacherId(idOf(lesson.getTeacher()));
        savedLesson.setTimeslotId(idOf(lesson.getTimeslot()));
        savedLesson.setRoomId(idOf(lesson.getRoom()));
        savedLesson.setLessonType(lesson.getLessonType());
        savedLesson.setPeriodicity(lesson.getPeriodicity() != null ? lesson.getPeriodicity() : Periodicity.WEEKLY);
        savedLesson.setSubgroup(lesson.getSubgroup());
        savedLesson.setSplitGroupIndex(lesson.getSplitGroupIndex());
        return savedLesson;
    }

    private SavedScheduleLesson copySavedScheduleLesson(SavedScheduleLesson source) {
        SavedScheduleLesson copy = new SavedScheduleLesson();
        copy.setLessonId(source.getLessonId());
        copy.setCoursePlanId(source.getCoursePlanId());
        copy.setGroupId(source.getGroupId());
        copy.setSubjectId(source.getSubjectId());
        copy.setTeacherId(source.getTeacherId());
        copy.setTimeslotId(source.getTimeslotId());
        copy.setRoomId(source.getRoomId());
        copy.setLessonType(source.getLessonType());
        copy.setPeriodicity(source.getPeriodicity());
        copy.setSubgroup(source.getSubgroup());
        copy.setSplitGroupIndex(source.getSplitGroupIndex());
        return copy;
    }

    private Optional<Lesson> resolveSavedLesson(
            SavedScheduleLesson savedLesson,
            Map<Long, Lesson> lessonsById,
            List<Lesson> lessons,
            Set<Long> restoredLessonIds) {
        Lesson directMatch = savedLesson.getLessonId() != null ? lessonsById.get(savedLesson.getLessonId()) : null;
        if (directMatch != null && directMatch.getId() != null && !restoredLessonIds.contains(directMatch.getId())) {
            return Optional.of(directMatch);
        }

        return lessons.stream()
                .filter(lesson -> lesson.getId() != null && !restoredLessonIds.contains(lesson.getId()))
                .filter(lesson -> matchesSavedLesson(lesson, savedLesson))
                .findFirst();
    }

    private boolean matchesSavedLesson(Lesson lesson, SavedScheduleLesson savedLesson) {
        boolean planMatches = sameId(idOf(lesson.getCoursePlan()), savedLesson.getCoursePlanId())
                || (sameId(idOf(lesson.getGroup()), savedLesson.getGroupId())
                && sameId(idOf(lesson.getSubject()), savedLesson.getSubjectId()));

        return planMatches
                && lesson.getLessonType() == savedLesson.getLessonType()
                && Objects.equals(defaultIndex(lesson.getSubgroup()), defaultIndex(savedLesson.getSubgroup()))
                && Objects.equals(defaultIndex(lesson.getSplitGroupIndex()), defaultIndex(savedLesson.getSplitGroupIndex()))
                && (savedLesson.getTeacherId() == null || sameId(idOf(lesson.getTeacher()), savedLesson.getTeacherId()));
    }

    private void applySavedLesson(Lesson lesson, SavedScheduleLesson savedLesson) {
        lesson.setTimeslot(savedLesson.getTimeslotId() != null
                ? timeslotRepository.findById(savedLesson.getTimeslotId()).orElse(null)
                : null);
        lesson.setRoom(savedLesson.getRoomId() != null
                ? roomRepository.findById(savedLesson.getRoomId()).orElse(null)
                : null);
        lesson.setPeriodicity(savedLesson.getPeriodicity() != null ? savedLesson.getPeriodicity() : Periodicity.WEEKLY);
    }

    private SavedScheduleDTO mapToSavedScheduleDTO(SavedSchedule savedSchedule) {
        return new SavedScheduleDTO(
                savedSchedule.getId(),
                savedSchedule.getName(),
                formatSavedScheduleDate(savedSchedule.getCreatedAt()),
                formatSavedScheduleDate(savedSchedule.getUpdatedAt()),
                savedSchedule.getLessons().size(),
                false,
                savedSchedule.isFullTemplate(),
                savedSchedule.isAutosaveEnabled()
        );
    }

    private String normalizeSavedScheduleName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Назва розкладу обов'язкова");
        }
        return name.trim();
    }

    private String formatSavedScheduleDate(LocalDateTime dateTime) {
        return dateTime != null ? SAVED_SCHEDULE_DATE_FORMAT.format(dateTime) : "";
    }

    private int getBuiltInTemplateLessonCount() {
        if (templateAccessService.isBaseTemplateLocked()) {
            return Math.toIntExact(lessonRepository.count());
        }
        return BUILT_IN_TEMPLATE_LESSON_COUNT;
    }

    private int nextSavedScheduleSortOrder() {
        return savedScheduleRepository.findAll().stream()
                .map(SavedSchedule::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    private void clearLessonPlacements() {
        List<Lesson> allLessons = lessonRepository.findAll();
        for (Lesson lesson : allLessons) {
            lesson.setTimeslot(null);
            lesson.setRoom(null);
            lessonRepository.save(lesson);
        }
    }

    private boolean isBuiltInTemplateName(String name) {
        return BUILT_IN_TEMPLATE_NAME.equalsIgnoreCase(name)
                || LEGACY_BUILT_IN_TEMPLATE_NAME.equalsIgnoreCase(name);
    }

    private void importBuiltInTemplate() {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    TRUNCATE TABLE
                        lesson,
                        teacher_competence_matrix,
                        course_plan,
                        student_group,
                        teacher,
                        subject,
                        room,
                        timeslot,
                        schedule_template,
                        schedule_profile
                    RESTART IDENTITY CASCADE
                    """);
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V2__Base_Template.sql"));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to import built-in schedule template.", e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private FullTemplateSnapshot captureCurrentSnapshot() {
        List<SubjectSnapshot> subjects = subjectRepository.findAll().stream()
                .sorted(Comparator.comparing(Subject::getId, Comparator.nullsLast(Long::compareTo)))
                .map(subject -> new SubjectSnapshot(subject.getId(), subject.getName(), subject.getAbbreviation()))
                .toList();

        List<RoomSnapshot> rooms = roomRepository.findAll().stream()
                .sorted(Comparator.comparing(Room::getId, Comparator.nullsLast(Long::compareTo)))
                .map(room -> new RoomSnapshot(room.getId(), room.getName(), room.getCapacity(), room.getBuilding(), room.getEquipment(), room.getType()))
                .toList();

        List<TeacherSnapshot> teachers = teacherRepository.findAll().stream()
                .sorted(Comparator.comparing(Teacher::getId, Comparator.nullsLast(Long::compareTo)))
                .map(teacher -> new TeacherSnapshot(
                        teacher.getId(),
                        teacher.getFullName(),
                        teacher.getDepartment(),
                        teacher.getSpecialization(),
                        teacher.getPositionType(),
                        teacher.getWeeklyHourLimit(),
                        teacher.getMaxWorkingDaysPerWeek(),
                        idOf(teacher.getAssignedRoom())))
                .toList();

        List<GroupSnapshot> groups = groupRepository.findAll().stream()
                .sorted(Comparator.comparing(Group::getId, Comparator.nullsLast(Long::compareTo)))
                .map(group -> new GroupSnapshot(group.getId(), group.getName(), group.getSize(), group.getCourse(), group.getDepartment(), group.getCuratorId()))
                .toList();

        List<CoursePlanSnapshot> coursePlans = coursePlanRepository.findAll().stream()
                .sorted(Comparator.comparing(CoursePlan::getId, Comparator.nullsLast(Long::compareTo)))
                .map(plan -> new CoursePlanSnapshot(
                        plan.getId(),
                        idOf(plan.getSubject()),
                        idOf(plan.getTeacher()),
                        idOf(plan.getSecondTeacher()),
                        idOf(plan.getGroup()),
                        plan.getTotalHours(),
                        plan.getLectureHours(),
                        plan.getPracticeHours(),
                        plan.getLabHours(),
                        plan.getLectureSessionsPerWeek(),
                        plan.getPracticeSessionsPerWeek(),
                        plan.getLabSessionsPerWeek(),
                        plan.getLecturePeriodicity(),
                        plan.getPracticePeriodicity(),
                        plan.getLabPeriodicity(),
                        plan.getExecutedHours(),
                        plan.getRequiredRoomType()))
                .toList();

        List<TimeslotSnapshot> timeslots = timeslotRepository.findAll().stream()
                .sorted(Comparator.comparing(Timeslot::getId, Comparator.nullsLast(Long::compareTo)))
                .map(timeslot -> new TimeslotSnapshot(
                        timeslot.getId(),
                        timeslot.getDayOfWeek(),
                        timeslot.getStartTime(),
                        timeslot.getEndTime(),
                        timeslot.getWeekParity(),
                        timeslot.getLessonNumber()))
                .toList();

        List<CompetenceSnapshot> competences = teacherCompetenceMatrixRepository.findAll().stream()
                .sorted(Comparator.comparing(TeacherCompetenceMatrix::getId, Comparator.nullsLast(Long::compareTo)))
                .map(matrix -> new CompetenceSnapshot(
                        matrix.getId(),
                        idOf(matrix.getTeacher()),
                        idOf(matrix.getSubject()),
                        matrix.getLessonType(),
                        matrix.getPriority()))
                .toList();

        List<LessonSnapshot> lessons = lessonRepository.findAll().stream()
                .sorted(Comparator.comparing(Lesson::getId, Comparator.nullsLast(Long::compareTo)))
                .map(lesson -> new LessonSnapshot(
                        lesson.getId(),
                        idOf(lesson.getSubject()),
                        lesson.getLessonType(),
                        idOf(lesson.getTeacher()),
                        idOf(lesson.getGroup()),
                        idOf(lesson.getCoursePlan()),
                        idOf(lesson.getTimeslot()),
                        idOf(lesson.getRoom()),
                        lesson.getPeriodicity(),
                        lesson.getSubgroup(),
                        lesson.getSplitGroupIndex()))
                .toList();

        return new FullTemplateSnapshot(subjects, rooms, teachers, groups, coursePlans, timeslots, competences, lessons);
    }

    private FullTemplateSnapshot clearSnapshotLessonPlacements(FullTemplateSnapshot snapshot) {
        List<LessonSnapshot> emptyLessons = snapshot.lessons().stream()
                .map(lesson -> new LessonSnapshot(
                        lesson.id(),
                        lesson.subjectId(),
                        lesson.lessonType(),
                        lesson.teacherId(),
                        lesson.groupId(),
                        lesson.coursePlanId(),
                        null,
                        null,
                        lesson.periodicity(),
                        lesson.subgroup(),
                        lesson.splitGroupIndex()))
                .toList();

        return new FullTemplateSnapshot(
                snapshot.subjects(),
                snapshot.rooms(),
                snapshot.teachers(),
                snapshot.groups(),
                snapshot.coursePlans(),
                snapshot.timeslots(),
                snapshot.competences(),
                emptyLessons);
    }

    private String serializeSnapshot(FullTemplateSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to save template snapshot.", e);
        }
    }

    private FullTemplateSnapshot deserializeSnapshot(String snapshotJson) {
        try {
            return objectMapper.readValue(snapshotJson, FullTemplateSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to read template snapshot.", e);
        }
    }

    private void restoreSnapshot(FullTemplateSnapshot snapshot) {
        clearWorkingData();

        Map<Long, Subject> subjectsByOldId = new HashMap<>();
        for (SubjectSnapshot source : snapshot.subjects()) {
            Subject subject = new Subject();
            subject.setName(source.name());
            subject.setAbbreviation(source.abbreviation());
            subjectsByOldId.put(source.id(), subjectRepository.save(subject));
        }

        Map<Long, Room> roomsByOldId = new HashMap<>();
        for (RoomSnapshot source : snapshot.rooms()) {
            Room room = new Room();
            room.setName(source.name());
            room.setCapacity(source.capacity());
            room.setBuilding(source.building());
            room.setEquipment(source.equipment());
            room.setType(source.type());
            roomsByOldId.put(source.id(), roomRepository.save(room));
        }

        Map<Long, Teacher> teachersByOldId = new HashMap<>();
        for (TeacherSnapshot source : snapshot.teachers()) {
            Teacher teacher = new Teacher();
            teacher.setFullName(source.fullName());
            teacher.setDepartment(source.department());
            teacher.setSpecialization(source.specialization());
            teacher.setPositionType(source.positionType());
            teacher.setWeeklyHourLimit(source.weeklyHourLimit());
            teacher.setMaxWorkingDaysPerWeek(source.maxWorkingDaysPerWeek());
            teacher.setAssignedRoom(roomsByOldId.get(source.assignedRoomId()));
            teachersByOldId.put(source.id(), teacherRepository.save(teacher));
        }

        Map<Long, Group> groupsByOldId = new HashMap<>();
        for (GroupSnapshot source : snapshot.groups()) {
            Group group = new Group();
            group.setName(source.name());
            group.setSize(source.size());
            group.setCourse(source.course());
            group.setDepartment(source.department());
            Teacher curator = teachersByOldId.get(source.curatorId());
            group.setCuratorId(curator != null ? curator.getId() : null);
            groupsByOldId.put(source.id(), groupRepository.save(group));
        }

        Map<Long, CoursePlan> coursePlansByOldId = new HashMap<>();
        for (CoursePlanSnapshot source : snapshot.coursePlans()) {
            CoursePlan plan = new CoursePlan();
            plan.setSubject(subjectsByOldId.get(source.subjectId()));
            plan.setTeacher(teachersByOldId.get(source.teacherId()));
            plan.setSecondTeacher(teachersByOldId.get(source.secondTeacherId()));
            plan.setGroup(groupsByOldId.get(source.groupId()));
            plan.setTotalHours(source.totalHours());
            plan.setLectureHours(source.lectureHours());
            plan.setPracticeHours(source.practiceHours());
            plan.setLabHours(source.labHours());
            plan.setLectureSessionsPerWeek(source.lectureSessionsPerWeek());
            plan.setPracticeSessionsPerWeek(source.practiceSessionsPerWeek());
            plan.setLabSessionsPerWeek(source.labSessionsPerWeek());
            plan.setLecturePeriodicity(source.lecturePeriodicity());
            plan.setPracticePeriodicity(source.practicePeriodicity());
            plan.setLabPeriodicity(source.labPeriodicity());
            plan.setExecutedHours(source.executedHours());
            plan.setRequiredRoomType(source.requiredRoomType());
            coursePlansByOldId.put(source.id(), coursePlanRepository.save(plan));
        }

        Map<Long, Timeslot> timeslotsByOldId = new HashMap<>();
        for (TimeslotSnapshot source : snapshot.timeslots()) {
            Timeslot timeslot = new Timeslot();
            timeslot.setDayOfWeek(source.dayOfWeek());
            timeslot.setStartTime(source.startTime());
            timeslot.setEndTime(source.endTime());
            timeslot.setWeekParity(source.weekParity());
            timeslot.setLessonNumber(source.lessonNumber());
            timeslotsByOldId.put(source.id(), timeslotRepository.save(timeslot));
        }

        for (CompetenceSnapshot source : snapshot.competences()) {
            TeacherCompetenceMatrix matrix = new TeacherCompetenceMatrix();
            matrix.setTeacher(teachersByOldId.get(source.teacherId()));
            matrix.setSubject(subjectsByOldId.get(source.subjectId()));
            matrix.setLessonType(source.lessonType());
            matrix.setPriority(source.priority());
            teacherCompetenceMatrixRepository.save(matrix);
        }

        for (LessonSnapshot source : snapshot.lessons()) {
            Lesson lesson = new Lesson();
            lesson.setSubject(subjectsByOldId.get(source.subjectId()));
            lesson.setLessonType(source.lessonType());
            lesson.setTeacher(teachersByOldId.get(source.teacherId()));
            lesson.setGroup(groupsByOldId.get(source.groupId()));
            lesson.setCoursePlan(coursePlansByOldId.get(source.coursePlanId()));
            lesson.setTimeslot(timeslotsByOldId.get(source.timeslotId()));
            lesson.setRoom(roomsByOldId.get(source.roomId()));
            lesson.setPeriodicity(source.periodicity() != null ? source.periodicity() : Periodicity.WEEKLY);
            lesson.setSubgroup(source.subgroup());
            lesson.setSplitGroupIndex(source.splitGroupIndex());
            lessonRepository.save(lesson);
        }
    }

    private void clearWorkingData() {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    TRUNCATE TABLE
                        lesson,
                        teacher_competence_matrix,
                        course_plan,
                        student_group,
                        teacher,
                        subject,
                        room,
                        timeslot,
                        schedule_template,
                        schedule_profile
                    RESTART IDENTITY CASCADE
                    """);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to clear active schedule data.", e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private record FullTemplateSnapshot(
            List<SubjectSnapshot> subjects,
            List<RoomSnapshot> rooms,
            List<TeacherSnapshot> teachers,
            List<GroupSnapshot> groups,
            List<CoursePlanSnapshot> coursePlans,
            List<TimeslotSnapshot> timeslots,
            List<CompetenceSnapshot> competences,
            List<LessonSnapshot> lessons
    ) {
    }

    private record SubjectSnapshot(Long id, String name, String abbreviation) {
    }

    private record RoomSnapshot(Long id, String name, Integer capacity, String building, String equipment, RoomType type) {
    }

    private record TeacherSnapshot(
            Long id,
            String fullName,
            String department,
            String specialization,
            PositionType positionType,
            Integer weeklyHourLimit,
            Integer maxWorkingDaysPerWeek,
            Long assignedRoomId
    ) {
    }

    private record GroupSnapshot(Long id, String name, Integer size, Integer course, String department, Long curatorId) {
    }

    private record CoursePlanSnapshot(
            Long id,
            Long subjectId,
            Long teacherId,
            Long secondTeacherId,
            Long groupId,
            Integer totalHours,
            Integer lectureHours,
            Integer practiceHours,
            Integer labHours,
            Integer lectureSessionsPerWeek,
            Integer practiceSessionsPerWeek,
            Integer labSessionsPerWeek,
            Periodicity lecturePeriodicity,
            Periodicity practicePeriodicity,
            Periodicity labPeriodicity,
            Integer executedHours,
            RoomType requiredRoomType
    ) {
    }

    private record TimeslotSnapshot(
            Long id,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Periodicity weekParity,
            Integer lessonNumber
    ) {
    }

    private record CompetenceSnapshot(
            Long id,
            Long teacherId,
            Long subjectId,
            LessonType lessonType,
            Priority priority
    ) {
    }

    private record LessonSnapshot(
            Long id,
            Long subjectId,
            LessonType lessonType,
            Long teacherId,
            Long groupId,
            Long coursePlanId,
            Long timeslotId,
            Long roomId,
            Periodicity periodicity,
            Integer subgroup,
            Integer splitGroupIndex
    ) {
    }

    private Integer defaultIndex(Integer value) {
        return value != null ? value : 0;
    }

    private Long idOf(Object entity) {
        if (entity instanceof Lesson lesson) return lesson.getId();
        if (entity instanceof CoursePlan coursePlan) return coursePlan.getId();
        if (entity instanceof Group group) return group.getId();
        if (entity instanceof Subject subject) return subject.getId();
        if (entity instanceof Teacher teacher) return teacher.getId();
        if (entity instanceof Timeslot timeslot) return timeslot.getId();
        if (entity instanceof Room room) return room.getId();
        return null;
    }

    private boolean sameId(Long firstId, Long secondId) {
        return firstId != null && firstId.equals(secondId);
    }

    @AnonymousAllowed
    @Transactional
    public void moveLesson(Long lessonId, Long timeslotId, String roomName, com.sergofoox.domain.plan.Periodicity periodicity) {
        try {
            if (published) {
                throw new IllegalStateException("Редагування заборонено: розклад опубліковано");
            }
            templateAccessService.requireWritableTemplate();
            Lesson primaryLesson = lessonRepository.findById(lessonId).orElseThrow();
            Timeslot newTimeslot = timeslotRepository.findById(timeslotId).orElseThrow();
            
            Timeslot oldTimeslot = primaryLesson.getTimeslot();
            com.sergofoox.domain.plan.Periodicity oldPeriodicity = primaryLesson.getPeriodicity();
            com.sergofoox.domain.plan.Periodicity requestedPeriodicity = periodicity != null ? periodicity : oldPeriodicity;
            com.sergofoox.domain.plan.Periodicity newPeriodicity = requestedPeriodicity;
            
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
            templateAccessService.requireWritableTemplate();
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
            templateAccessService.requireWritableTemplate();
            Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
            
            if (subjectId != null) {
                Subject subject = subjectRepository.findById(subjectId).orElseThrow();
                lesson.setSubject(subject);
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
            templateAccessService.requireWritableTemplate();
            
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

    private GroupDTO mapToGroupDTO(Group group, Map<Long, Teacher> teachersById) {
        String curatorName = null;
        if (group.getCuratorId() != null) {
            Teacher curator = teachersById.get(group.getCuratorId());
            if (curator != null) {
                curatorName = curator.getFullName();
            }
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

    private LessonDTO mapToLessonDTO(Lesson lesson, List<Lesson> sameSlotLessons) {
        boolean hasConflict = false;
        if (lesson.getTimeslot() != null && lesson.getId() != null) {
            hasConflict = sameSlotLessons.stream()
                    .filter(l -> l.getId() != null && !l.getId().equals(lesson.getId()))
                    .anyMatch(l -> {
                        if (!weeksOverlap(l, lesson)) return false;

                        boolean teacherConflict = l.getTeacher() != null && lesson.getTeacher() != null && 
                                                 l.getTeacher().getId().equals(lesson.getTeacher().getId());
                        
                        boolean roomConflict = l.getRoom() != null && lesson.getRoom() != null && 
                                              l.getRoom().getId().equals(lesson.getRoom().getId());
                        
                        boolean subjectConflict = l.getSubject() != null && lesson.getSubject() != null
                                && l.getSubject().getId().equals(lesson.getSubject().getId())
                                && !sameSplitGroupLesson(l, lesson);
                        
                        boolean groupConflict = l.getGroup() != null && lesson.getGroup() != null
                                && l.getGroup().getId().equals(lesson.getGroup().getId())
                                && !sameSplitGroupLesson(l, lesson);
                        
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
