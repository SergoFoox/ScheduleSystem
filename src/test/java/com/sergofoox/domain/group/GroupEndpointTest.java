package com.sergofoox.domain.group;

import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.plan.CoursePlan;
import com.sergofoox.domain.plan.CoursePlanRepository;
import com.sergofoox.domain.plan.RoomType;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.teacher.PositionType;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupEndpointTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CoursePlanRepository coursePlanRepository;

    private GroupEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new GroupEndpoint(
                groupRepository,
                teacherRepository,
                lessonRepository,
                coursePlanRepository
        );
    }

    @Test
    void deleteGroupDeletesLessonsAndCoursePlansBeforeGroup() {
        Group group = new Group(7L, "KB-41", 25, 4, "CS");
        Subject subject = new Subject(1L, "Math", "M");
        Teacher teacher = new Teacher(1L, "Smith", "CS", PositionType.FULL_TIME, 40);
        CoursePlan coursePlan = new CoursePlan(subject, teacher, group, 32, 16, 16, 0, 1, 1, 0, RoomType.LECTURE_HALL);

        when(groupRepository.findById(7L)).thenReturn(Optional.of(group));
        when(coursePlanRepository.findByGroup(group)).thenReturn(List.of(coursePlan));

        endpoint.deleteGroup(7L);

        InOrder inOrder = inOrder(coursePlanRepository, lessonRepository, groupRepository);
        inOrder.verify(groupRepository).findById(7L);
        inOrder.verify(coursePlanRepository).findByGroup(group);
        inOrder.verify(lessonRepository).deleteByGroup(group);
        inOrder.verify(lessonRepository).deleteByCoursePlan(coursePlan);
        inOrder.verify(coursePlanRepository).deleteByGroup(group);
        inOrder.verify(groupRepository).delete(group);
    }
}
