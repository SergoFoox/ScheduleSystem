package com.sergofoox.domain.solver;

import com.sergofoox.domain.group.GroupRepository;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.room.RoomRepository;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.timeslot.TimeslotRepository;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ScheduleService {

    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final SolverManager<Schedule, UUID> solverManager;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final RoomRepository roomRepository;
    private final TimeslotRepository timeslotRepository;
    private final LessonRepository lessonRepository;

    public ScheduleService(SolverManager<Schedule, UUID> solverManager,
                           TeacherRepository teacherRepository,
                           GroupRepository groupRepository,
                           RoomRepository roomRepository,
                           TimeslotRepository timeslotRepository,
                           LessonRepository lessonRepository) {
        this.solverManager = solverManager;
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.roomRepository = roomRepository;
        this.timeslotRepository = timeslotRepository;
        this.lessonRepository = lessonRepository;
    }

    @Transactional
    public void solve() {
        solverManager.solve(SINGLETON_ID,
                this::findById,
                this::save);
    }

    public Schedule findById(UUID id) {
        return new Schedule(
                timeslotRepository.findAll(),
                roomRepository.findAll(),
                lessonRepository.findAll()
        );
    }

    @Transactional
    public void save(Schedule schedule) {
        for (Lesson lesson : schedule.getLessons()) {
            // Only update the planning variables
            Lesson databaseLesson = lessonRepository.findById(lesson.getId()).orElseThrow();
            databaseLesson.setTimeslot(lesson.getTimeslot());
            databaseLesson.setRoom(lesson.getRoom());
            lessonRepository.save(databaseLesson);
        }
    }

    public SolverStatus getSolverStatus() {
        return solverManager.getSolverStatus(SINGLETON_ID);
    }

    public void stopSolving() {
        solverManager.terminateEarly(SINGLETON_ID);
    }
}
