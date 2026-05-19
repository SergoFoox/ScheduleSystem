package com.sergofoox.domain.autosave;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.sergofoox.domain.saved.SavedSchedule;
import com.sergofoox.domain.saved.SavedScheduleLesson;
import com.sergofoox.domain.saved.SavedScheduleRepository;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.SubjectRepository;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherAvailability;
import com.sergofoox.domain.teacher.TeacherAvailabilityRepository;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.timeslot.Timeslot;
import com.sergofoox.domain.timeslot.TimeslotRepository;
import com.sergofoox.domain.ui.TemplateAccessService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class AutosaveService {

    private final AutosaveRepository autosaveRepository;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final RoomRepository roomRepository;
    private final SubjectRepository subjectRepository;
    private final CoursePlanRepository coursePlanRepository;
    private final LessonRepository lessonRepository;
    private final SavedScheduleRepository savedScheduleRepository;
    private final TimeslotRepository timeslotRepository;
    private final TeacherCompetenceMatrixRepository teacherCompetenceMatrixRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;
    private final ObjectMapper objectMapper;
    private final TemplateAccessService templateAccessService;
    private final Executor snapshotExecutor = Executors.newSingleThreadExecutor();

    public AutosaveService(AutosaveRepository autosaveRepository,
                           TeacherRepository teacherRepository,
                           GroupRepository groupRepository,
                           RoomRepository roomRepository,
                           SubjectRepository subjectRepository,
                           CoursePlanRepository coursePlanRepository,
                           LessonRepository lessonRepository,
                           SavedScheduleRepository savedScheduleRepository,
                           TimeslotRepository timeslotRepository,
                           TeacherCompetenceMatrixRepository teacherCompetenceMatrixRepository,
                           TeacherAvailabilityRepository teacherAvailabilityRepository,
                           ObjectMapper objectMapper,
                           TemplateAccessService templateAccessService) {
        this.autosaveRepository = autosaveRepository;
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.roomRepository = roomRepository;
        this.subjectRepository = subjectRepository;
        this.coursePlanRepository = coursePlanRepository;
        this.lessonRepository = lessonRepository;
        this.savedScheduleRepository = savedScheduleRepository;
        this.timeslotRepository = timeslotRepository;
        this.teacherCompetenceMatrixRepository = teacherCompetenceMatrixRepository;
        this.teacherAvailabilityRepository = teacherAvailabilityRepository;
        this.objectMapper = objectMapper;
        this.templateAccessService = templateAccessService;
    }

    public void captureSnapshotAsync(boolean isManual) {
        CompletableFuture.runAsync(() -> captureSnapshot(isManual), snapshotExecutor);
    }

    public void captureSnapshotAfterCommitAsync(boolean isManual) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            captureSnapshotAsync(isManual);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                captureSnapshotAsync(isManual);
            }
        });
    }

    @Transactional
    public void captureSnapshot(boolean isManual) {
        try {
            Long activeId = templateAccessService.getActiveSavedScheduleId();
            
            // Skip autosave when the active schedule is the base template (null).
            if (activeId == null) {
                if (!isManual) return;
            } else {
                // Check whether autosave is enabled for the active schedule.
                SavedSchedule schedule = savedScheduleRepository.findById(activeId).orElse(null);
                if (!isManual && schedule != null && !schedule.isAutosaveEnabled()) {
                    return;
                }
            }

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("teachers", teacherRepository.findAll().stream().map(this::mapTeacher).toList());
            snapshot.put("groups", groupRepository.findAll().stream().map(this::mapGroup).toList());
            snapshot.put("rooms", roomRepository.findAll().stream().map(this::mapRoom).toList());
            snapshot.put("subjects", subjectRepository.findAll().stream().map(this::mapSubject).toList());
            snapshot.put("coursePlans", coursePlanRepository.findAll().stream().map(this::mapCoursePlan).toList());
            snapshot.put("lessons", lessonRepository.findAll().stream().map(this::mapLesson).toList());
            snapshot.put("matrix", teacherCompetenceMatrixRepository.findAll().stream().map(this::mapMatrix).toList());
            snapshot.put("availability", teacherAvailabilityRepository.findAll().stream().map(this::mapAvailability).toList());

            String json = objectMapper.writeValueAsString(snapshot);
            int totalCount = snapshot.values().stream()
                    .filter(v -> v instanceof List)
                    .mapToInt(v -> ((List<?>) v).size())
                    .sum();

            AutosaveSnapshot entity = new AutosaveSnapshot(LocalDateTime.now(), json, totalCount, isManual, activeId);
            autosaveRepository.save(entity);

            List<AutosaveSnapshot> scheduleSnapshots = autosaveRepository.findByScheduleIdOrderByTimestampDesc(activeId);
            List<AutosaveSnapshot> autoSnapshots = scheduleSnapshots.stream()
                    .filter(s -> !s.isManual())
                    .toList();
            
            if (autoSnapshots.size() > 10) {
                List<AutosaveSnapshot> toDelete = autoSnapshots.subList(10, autoSnapshots.size());
                autosaveRepository.deleteAll(toDelete);
            }
        } catch (Exception e) {
            System.err.println(">>> Помилка при створенні знімка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Map<String, Object> mapTeacher(Teacher t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("fullName", t.getFullName());
        m.put("department", t.getDepartment());
        m.put("specialization", t.getSpecialization());
        m.put("positionType", t.getPositionType());
        m.put("weeklyHourLimit", t.getWeeklyHourLimit());
        m.put("maxWorkingDaysPerWeek", t.getMaxWorkingDaysPerWeek());
        m.put("assignedRoomId", t.getAssignedRoom() != null ? t.getAssignedRoom().getId() : null);
        m.put("archived", t.isArchived());
        return m;
    }

    private Map<String, Object> mapGroup(Group g) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", g.getId());
        m.put("name", g.getName());
        m.put("size", g.getSize());
        m.put("course", g.getCourse());
        m.put("department", g.getDepartment());
        m.put("curatorId", g.getCuratorId());
        return m;
    }

    private Map<String, Object> mapRoom(Room r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("name", r.getName());
        m.put("capacity", r.getCapacity());
        m.put("building", r.getBuilding());
        m.put("equipment", r.getEquipment());
        m.put("type", r.getType());
        return m;
    }

    private Map<String, Object> mapSubject(Subject s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("abbreviation", s.getAbbreviation());
        return m;
    }

    private Map<String, Object> mapCoursePlan(CoursePlan cp) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", cp.getId());
        m.put("subjectId", cp.getSubject() != null ? cp.getSubject().getId() : null);
        m.put("teacherId", cp.getTeacher() != null ? cp.getTeacher().getId() : null);
        m.put("secondTeacherId", cp.getSecondTeacher() != null ? cp.getSecondTeacher().getId() : null);
        m.put("groupId", cp.getGroup() != null ? cp.getGroup().getId() : null);
        m.put("totalHours", cp.getTotalHours());
        m.put("lectureHours", cp.getLectureHours());
        m.put("practiceHours", cp.getPracticeHours());
        m.put("labHours", cp.getLabHours());
        m.put("lectureSessionsPerWeek", cp.getLectureSessionsPerWeek());
        m.put("practiceSessionsPerWeek", cp.getPracticeSessionsPerWeek());
        m.put("labSessionsPerWeek", cp.getLabSessionsPerWeek());
        m.put("lecturePeriodicity", cp.getLecturePeriodicity());
        m.put("practicePeriodicity", cp.getPracticePeriodicity());
        m.put("labPeriodicity", cp.getLabPeriodicity());
        m.put("executedHours", cp.getExecutedHours());
        m.put("requiredRoomType", cp.getRequiredRoomType());
        return m;
    }

    private Map<String, Object> mapLesson(Lesson l) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", l.getId());
        m.put("subjectId", l.getSubject() != null ? l.getSubject().getId() : null);
        m.put("teacherId", l.getTeacher() != null ? l.getTeacher().getId() : null);
        m.put("groupId", l.getGroup() != null ? l.getGroup().getId() : null);
        m.put("coursePlanId", l.getCoursePlan() != null ? l.getCoursePlan().getId() : null);
        m.put("timeslotId", l.getTimeslot() != null ? l.getTimeslot().getId() : null);
        m.put("roomId", l.getRoom() != null ? l.getRoom().getId() : null);
        m.put("lessonType", l.getLessonType());
        m.put("periodicity", l.getPeriodicity());
        m.put("subgroup", l.getSubgroup());
        m.put("splitGroupIndex", l.getSplitGroupIndex());
        return m;
    }

    private Map<String, Object> mapMatrix(TeacherCompetenceMatrix tcm) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", tcm.getId());
        m.put("teacherId", tcm.getTeacher() != null ? tcm.getTeacher().getId() : null);
        m.put("subjectId", tcm.getSubject() != null ? tcm.getSubject().getId() : null);
        m.put("lessonType", tcm.getLessonType());
        m.put("priority", tcm.getPriority());
        return m;
    }

    private Map<String, Object> mapAvailability(TeacherAvailability availability) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", availability.getId());
        m.put("teacherId", availability.getTeacher() != null ? availability.getTeacher().getId() : null);
        m.put("dayOfWeek", availability.getDayOfWeek());
        m.put("lessonNumber", availability.getLessonNumber());
        m.put("status", availability.getStatus());
        return m;
    }

    @Scheduled(fixedRate = 120000, initialDelay = 60000)
    public void autoSaveTask() {
        captureSnapshotAsync(false);
    }

    @Transactional
    public void restoreSnapshot(Long snapshotId, boolean asNewTemplate) {
        AutosaveSnapshot snapshot = autosaveRepository.findById(snapshotId)
                .orElseThrow(() -> new IllegalArgumentException("Snapshot not found"));

        try {
            Map<String, List<Map<String, Object>>> data = objectMapper.readValue(
                    snapshot.getSnapshotData(), new TypeReference<>() {});

            if (asNewTemplate) {
                restoreAsNewTemplate(snapshot, data);
            } else {
                restoreAsFullRollback(data);
                activateSnapshotSchedule(snapshot);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Помилка десеріалізації знімка", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean restoreLatestSnapshotAsCurrent() {
        AutosaveSnapshot snapshot = autosaveRepository.findFirstByOrderByTimestampDesc().orElse(null);
        if (snapshot == null) {
            return false;
        }

        try {
            Map<String, List<Map<String, Object>>> data = objectMapper.readValue(
                    snapshot.getSnapshotData(), new TypeReference<>() {});
            restoreAsFullRollback(data);
            activateSnapshotSchedule(snapshot);
            return true;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize autosave snapshot", e);
        }
    }

    private void activateSnapshotSchedule(AutosaveSnapshot snapshot) {
        Long scheduleId = snapshot.getScheduleId();
        if (scheduleId != null && savedScheduleRepository.existsById(scheduleId)) {
            templateAccessService.activateEditableTemplate(scheduleId);
        } else {
            templateAccessService.resetBaseTemplateSession();
        }
    }

    private void restoreAsNewTemplate(AutosaveSnapshot snapshot, Map<String, List<Map<String, Object>>> data) {
        SavedSchedule savedSchedule = new SavedSchedule();
        String timestamp = snapshot.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        savedSchedule.setName("Відновлено " + timestamp);
        savedSchedule.setCreatedAt(LocalDateTime.now());
        savedSchedule.setUpdatedAt(LocalDateTime.now());
        savedSchedule.setSnapshotJson(snapshot.getSnapshotData());
        savedSchedule.setFullTemplate(true);

        List<Map<String, Object>> lessonsData = data.get("lessons");
        List<SavedScheduleLesson> lessons = new ArrayList<>();

        if (lessonsData != null) {
            for (Map<String, Object> lData : lessonsData) {
                SavedScheduleLesson sl = new SavedScheduleLesson();
                sl.setSavedSchedule(savedSchedule);
                sl.setLessonId(getLong(lData, "id"));
                sl.setCoursePlanId(getLong(lData, "coursePlanId"));
                sl.setSubjectId(getLong(lData, "subjectId"));
                sl.setTeacherId(getLong(lData, "teacherId"));
                sl.setGroupId(getLong(lData, "groupId"));
                sl.setTimeslotId(getLong(lData, "timeslotId"));
                sl.setRoomId(getLong(lData, "roomId"));
                
                String typeStr = (String) lData.get("lessonType");
                if (typeStr != null) {
                    sl.setLessonType(com.sergofoox.domain.subject.LessonType.valueOf(typeStr));
                }
                
                String periodicityStr = (String) lData.get("periodicity");
                if (periodicityStr != null) {
                    sl.setPeriodicity(com.sergofoox.domain.plan.Periodicity.valueOf(periodicityStr));
                } else {
                    sl.setPeriodicity(com.sergofoox.domain.plan.Periodicity.WEEKLY);
                }
                
                sl.setSubgroup((Integer) lData.getOrDefault("subgroup", 0));
                sl.setSplitGroupIndex((Integer) lData.getOrDefault("splitGroupIndex", 0));
                
                lessons.add(sl);
            }
        }

        savedSchedule.setLessons(lessons);
        savedScheduleRepository.save(savedSchedule);
    }

    private void restoreAsFullRollback(Map<String, List<Map<String, Object>>> data) {
        lessonRepository.deleteAllInBatch();
        teacherAvailabilityRepository.deleteAllInBatch();
        teacherCompetenceMatrixRepository.deleteAllInBatch();
        coursePlanRepository.deleteAllInBatch();
        teacherRepository.deleteAllInBatch();
        groupRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        subjectRepository.deleteAllInBatch();

        Map<Long, Subject> subjectsMap = new HashMap<>();
        List<Map<String, Object>> subjectsData = data.get("subjects");
        if (subjectsData != null) {
            for (Map<String, Object> sData : subjectsData) {
                Subject s = new Subject();
                s.setName((String) sData.get("name"));
                s.setAbbreviation((String) sData.get("abbreviation"));
                subjectsMap.put(getLong(sData, "id"), subjectRepository.save(s));
            }
        }

        Map<Long, Room> roomsMap = new HashMap<>();
        List<Map<String, Object>> roomsData = data.get("rooms");
        if (roomsData != null) {
            for (Map<String, Object> rData : roomsData) {
                Room r = new Room();
                r.setName((String) rData.get("name"));
                r.setCapacity((Integer) rData.get("capacity"));
                r.setBuilding((String) rData.get("building"));
                r.setEquipment((String) rData.get("equipment"));
                String typeStr = (String) rData.get("type");
                if (typeStr != null) {
                    r.setType(com.sergofoox.domain.plan.RoomType.valueOf(typeStr));
                }
                roomsMap.put(getLong(rData, "id"), roomRepository.save(r));
            }
        }

        Map<Long, Teacher> teachersMap = new HashMap<>();
        List<Map<String, Object>> teachersData = data.get("teachers");
        if (teachersData != null) {
            for (Map<String, Object> tData : teachersData) {
                Teacher t = new Teacher();
                t.setFullName((String) tData.get("fullName"));
                t.setDepartment((String) tData.get("department"));
                t.setSpecialization((String) tData.get("specialization"));
                String posTypeStr = (String) tData.get("positionType");
                if (posTypeStr != null) {
                    t.setPositionType(com.sergofoox.domain.teacher.PositionType.valueOf(posTypeStr));
                }
                t.setWeeklyHourLimit((Integer) tData.get("weeklyHourLimit"));
                t.setMaxWorkingDaysPerWeek((Integer) tData.get("maxWorkingDaysPerWeek"));
                t.setArchived(Boolean.TRUE.equals(tData.get("archived")));
                
                Long roomId = getLong(tData, "assignedRoomId");
                if (roomId != null) {
                    t.setAssignedRoom(roomsMap.get(roomId));
                }
                
                teachersMap.put(getLong(tData, "id"), teacherRepository.save(t));
            }
        }

        List<Map<String, Object>> availabilityData = data.get("availability");
        if (availabilityData != null) {
            for (Map<String, Object> aData : availabilityData) {
                Teacher teacher = teachersMap.get(getLong(aData, "teacherId"));
                if (teacher == null) {
                    continue;
                }

                TeacherAvailability availability = new TeacherAvailability();
                availability.setTeacher(teacher);

                String dayOfWeekStr = (String) aData.get("dayOfWeek");
                if (dayOfWeekStr != null) {
                    availability.setDayOfWeek(java.time.DayOfWeek.valueOf(dayOfWeekStr));
                }

                availability.setLessonNumber((Integer) aData.get("lessonNumber"));

                String statusStr = (String) aData.get("status");
                if (statusStr != null) {
                    availability.setStatus(com.sergofoox.domain.teacher.AvailabilityStatus.valueOf(statusStr));
                }

                teacherAvailabilityRepository.save(availability);
            }
        }

        Map<Long, Group> groupsMap = new HashMap<>();
        List<Map<String, Object>> groupsData = data.get("groups");
        if (groupsData != null) {
            for (Map<String, Object> gData : groupsData) {
                Group g = new Group();
                g.setName((String) gData.get("name"));
                g.setSize((Integer) gData.get("size"));
                g.setCourse((Integer) gData.get("course"));
                g.setDepartment((String) gData.get("department"));
                
                Long curatorId = getLong(gData, "curatorId");
                if (curatorId != null) {
                    Teacher curator = teachersMap.get(curatorId);
                    g.setCuratorId(curator != null ? curator.getId() : null);
                }
                
                groupsMap.put(getLong(gData, "id"), groupRepository.save(g));
            }
        }

        Map<Long, CoursePlan> plansMap = new HashMap<>();
        List<Map<String, Object>> plansData = data.get("coursePlans");
        if (plansData != null) {
            for (Map<String, Object> pData : plansData) {
                CoursePlan cp = new CoursePlan();
                cp.setSubject(subjectsMap.get(getLong(pData, "subjectId")));
                cp.setTeacher(teachersMap.get(getLong(pData, "teacherId")));
                cp.setSecondTeacher(teachersMap.get(getLong(pData, "secondTeacherId")));
                cp.setGroup(groupsMap.get(getLong(pData, "groupId")));
                cp.setTotalHours((Integer) pData.get("totalHours"));
                cp.setLectureHours((Integer) pData.get("lectureHours"));
                cp.setPracticeHours((Integer) pData.get("practiceHours"));
                cp.setLabHours((Integer) pData.get("labHours"));
                cp.setLectureSessionsPerWeek((Integer) pData.get("lectureSessionsPerWeek"));
                cp.setPracticeSessionsPerWeek((Integer) pData.get("practiceSessionsPerWeek"));
                cp.setLabSessionsPerWeek((Integer) pData.get("labSessionsPerWeek"));
                
                String lPeriodStr = (String) pData.get("lecturePeriodicity");
                if (lPeriodStr != null) cp.setLecturePeriodicity(com.sergofoox.domain.plan.Periodicity.valueOf(lPeriodStr));
                
                String pPeriodStr = (String) pData.get("practicePeriodicity");
                if (pPeriodStr != null) cp.setPracticePeriodicity(com.sergofoox.domain.plan.Periodicity.valueOf(pPeriodStr));
                
                String labPeriodStr = (String) pData.get("labPeriodicity");
                if (labPeriodStr != null) cp.setLabPeriodicity(com.sergofoox.domain.plan.Periodicity.valueOf(labPeriodStr));
                
                cp.setExecutedHours((Integer) pData.getOrDefault("executedHours", 0));
                
                String rTypeStr = (String) pData.get("requiredRoomType");
                if (rTypeStr != null) cp.setRequiredRoomType(com.sergofoox.domain.plan.RoomType.valueOf(rTypeStr));
                
                plansMap.put(getLong(pData, "id"), coursePlanRepository.save(cp));
            }
        }

        List<Map<String, Object>> matrixData = data.get("matrix");
        if (matrixData != null) {
            for (Map<String, Object> mData : matrixData) {
                TeacherCompetenceMatrix m = new TeacherCompetenceMatrix();
                m.setTeacher(teachersMap.get(getLong(mData, "teacherId")));
                m.setSubject(subjectsMap.get(getLong(mData, "subjectId")));
                
                String typeStr = (String) mData.get("lessonType");
                if (typeStr != null) m.setLessonType(com.sergofoox.domain.subject.LessonType.valueOf(typeStr));
                
                String priorityStr = (String) mData.get("priority");
                if (priorityStr != null) m.setPriority(com.sergofoox.domain.competence.Priority.valueOf(priorityStr));
                
                teacherCompetenceMatrixRepository.save(m);
            }
        }

        List<Map<String, Object>> lessonsData = data.get("lessons");
        if (lessonsData != null) {
            for (Map<String, Object> lData : lessonsData) {
                Lesson l = new Lesson();
                l.setSubject(subjectsMap.get(getLong(lData, "subjectId")));
                l.setTeacher(teachersMap.get(getLong(lData, "teacherId")));
                l.setGroup(groupsMap.get(getLong(lData, "groupId")));
                l.setCoursePlan(plansMap.get(getLong(lData, "coursePlanId")));
                
                String typeStr = (String) lData.get("lessonType");
                if (typeStr != null) l.setLessonType(com.sergofoox.domain.subject.LessonType.valueOf(typeStr));
                
                Long tsId = getLong(lData, "timeslotId");
                if (tsId != null) {
                    l.setTimeslot(timeslotRepository.findById(tsId).orElse(null));
                }
                
                Long rId = getLong(lData, "roomId");
                if (rId != null) {
                    l.setRoom(roomsMap.get(rId));
                }
                
                String periodicityStr = (String) lData.get("periodicity");
                if (periodicityStr != null) {
                    l.setPeriodicity(com.sergofoox.domain.plan.Periodicity.valueOf(periodicityStr));
                }
                
                l.setSubgroup((Integer) lData.getOrDefault("subgroup", 0));
                l.setSplitGroupIndex((Integer) lData.getOrDefault("splitGroupIndex", 0));
                
                lessonRepository.save(l);
            }
        }
    }

    @Transactional
    public void deleteSnapshot(Long id) {
        autosaveRepository.deleteById(id);
    }

    @Transactional
    public void deleteSnapshotsForSchedule(Long scheduleId) {
        if (scheduleId != null) {
            autosaveRepository.deleteByScheduleId(scheduleId);
        }
    }

    @Transactional
    public void copySnapshots(Long sourceId, Long targetId) {
        if (sourceId == null || targetId == null) return;
        
        List<AutosaveSnapshot> sourceSnapshots = autosaveRepository.findByScheduleIdOrderByTimestampDesc(sourceId);
        for (AutosaveSnapshot source : sourceSnapshots) {
            AutosaveSnapshot copy = new AutosaveSnapshot(
                source.getTimestamp(),
                source.getSnapshotData(),
                source.getEntityCount(),
                source.isManual(),
                targetId
            );
            autosaveRepository.save(copy);
        }
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
}
