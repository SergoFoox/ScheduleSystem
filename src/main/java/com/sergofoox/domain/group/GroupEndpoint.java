package com.sergofoox.domain.group;

import com.sergofoox.domain.ui.dto.GroupDTO;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.plan.CoursePlan;
import com.sergofoox.domain.plan.CoursePlanRepository;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@BrowserCallable
@Service
@AnonymousAllowed
public class GroupEndpoint {

    private final GroupRepository groupRepository;
    private final TeacherRepository teacherRepository;
    private final LessonRepository lessonRepository;
    private final CoursePlanRepository coursePlanRepository;

    public GroupEndpoint(GroupRepository groupRepository,
                         TeacherRepository teacherRepository,
                         LessonRepository lessonRepository,
                         CoursePlanRepository coursePlanRepository) {
        this.groupRepository = groupRepository;
        this.teacherRepository = teacherRepository;
        this.lessonRepository = lessonRepository;
        this.coursePlanRepository = coursePlanRepository;
    }

    public List<GroupDTO> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public void saveGroup(GroupDTO dto) {
        System.out.println("Attempting to save group: " + dto.name());
        try {
            Group group;
            if (dto.id() != null) {
                group = groupRepository.findById(dto.id()).orElseThrow();
            } else {
                group = new Group();
            }
            
            group.setName(dto.name());
            group.setSize(dto.size());
            group.setCourse(dto.course());
            group.setDepartment(dto.department());
            
            if (dto.curatorId() != null) {
                group.setCuratorId(dto.curatorId());
            }
            
            groupRepository.save(group);
            System.out.println("Group saved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public void deleteGroup(Long id) {
        try {
            Group group = groupRepository.findById(id).orElseThrow();
            List<CoursePlan> coursePlans = coursePlanRepository.findByGroup(group);

            lessonRepository.deleteByGroup(group);
            for (CoursePlan coursePlan : coursePlans) {
                lessonRepository.deleteByCoursePlan(coursePlan);
            }
            coursePlanRepository.deleteByGroup(group);
            groupRepository.delete(group);
            System.out.println("Group deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private GroupDTO mapToDTO(Group group) {
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
}
