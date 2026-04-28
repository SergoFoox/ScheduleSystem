package com.sergofoox.domain.plan;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.group.GroupRepository;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.SubjectRepository;
import com.sergofoox.domain.teacher.PositionType;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoursePlanEndpointTest {

    @Mock
    private CoursePlanRepository coursePlanRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private LessonRepository lessonRepository;

    private CoursePlanEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new CoursePlanEndpoint(
                coursePlanRepository,
                groupRepository,
                subjectRepository,
                teacherRepository,
                lessonRepository
        );
    }

    @Test
    void copyPlansFromGroupCopiesMissingSubjectsOnly() {
        Group sourceGroup = new Group(1L, "KB-41", 25, 4, "CS");
        Group targetGroup = new Group(2L, "KB-42", 25, 4, "CS");
        Subject math = new Subject(10L, "Math", "M");
        Subject physics = new Subject(11L, "Physics", "PH");
        Teacher teacher = new Teacher(20L, "Smith", "CS", PositionType.FULL_TIME, 40);
        Teacher secondTeacher = new Teacher(21L, "Jones", "CS", PositionType.PART_TIME, 20);

        CoursePlan sourceMath = new CoursePlan(math, teacher, sourceGroup, 64, 32, 16, 16, 2, 1, 1, RoomType.LABORATORY);
        sourceMath.setSecondTeacher(secondTeacher);
        sourceMath.setLecturePeriodicity(Periodicity.ODD_WEEKS);
        sourceMath.setPracticePeriodicity(Periodicity.EVEN_WEEKS);
        sourceMath.setLabPeriodicity(Periodicity.WEEKLY);
        sourceMath.setExecutedHours(12);

        CoursePlan sourcePhysics = new CoursePlan(physics, teacher, sourceGroup, 32, 16, 16, 0, 1, 1, 0, RoomType.LECTURE_HALL);
        CoursePlan existingPhysics = new CoursePlan(physics, teacher, targetGroup, 32, 16, 16, 0, 1, 1, 0, RoomType.LECTURE_HALL);

        when(groupRepository.findById(1L)).thenReturn(Optional.of(sourceGroup));
        when(groupRepository.findById(2L)).thenReturn(Optional.of(targetGroup));
        when(coursePlanRepository.findByGroup(targetGroup)).thenReturn(List.of(existingPhysics));
        when(coursePlanRepository.findByGroup(sourceGroup)).thenReturn(List.of(sourceMath, sourcePhysics));

        int copiedCount = endpoint.copyPlansFromGroup(1L, 2L);

        assertEquals(1, copiedCount);

        ArgumentCaptor<CoursePlan> captor = ArgumentCaptor.forClass(CoursePlan.class);
        verify(coursePlanRepository).save(captor.capture());

        CoursePlan copiedPlan = captor.getValue();
        assertSame(targetGroup, copiedPlan.getGroup());
        assertSame(math, copiedPlan.getSubject());
        assertSame(teacher, copiedPlan.getTeacher());
        assertSame(secondTeacher, copiedPlan.getSecondTeacher());
        assertEquals(64, copiedPlan.getTotalHours());
        assertEquals(32, copiedPlan.getLectureHours());
        assertEquals(16, copiedPlan.getPracticeHours());
        assertEquals(16, copiedPlan.getLabHours());
        assertEquals(2, copiedPlan.getLectureSessionsPerWeek());
        assertEquals(1, copiedPlan.getPracticeSessionsPerWeek());
        assertEquals(1, copiedPlan.getLabSessionsPerWeek());
        assertEquals(RoomType.LABORATORY, copiedPlan.getRequiredRoomType());
        assertEquals(Periodicity.ODD_WEEKS, copiedPlan.getLecturePeriodicity());
        assertEquals(Periodicity.EVEN_WEEKS, copiedPlan.getPracticePeriodicity());
        assertEquals(Periodicity.WEEKLY, copiedPlan.getLabPeriodicity());
        assertEquals(0, copiedPlan.getExecutedHours());
    }

    @Test
    void copyPlansFromGroupRejectsSameGroup() {
        assertThrows(IllegalArgumentException.class, () -> endpoint.copyPlansFromGroup(1L, 1L));
    }
}
