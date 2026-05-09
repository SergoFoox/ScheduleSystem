package com.sergofoox.config;

import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.group.GroupRepository;
import com.sergofoox.domain.room.RoomRepository;
import com.sergofoox.domain.timeslot.Timeslot;
import com.sergofoox.domain.timeslot.TimeslotRepository;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.subject.SubjectRepository;
import com.sergofoox.domain.plan.CoursePlanRepository;
import com.sergofoox.domain.competence.TeacherCompetenceMatrixRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(
            TeacherRepository teacherRepository,
            GroupRepository groupRepository,
            RoomRepository roomRepository,
            TimeslotRepository timeslotRepository,
            LessonRepository lessonRepository,
            SubjectRepository subjectRepository,
            CoursePlanRepository coursePlanRepository,
            TeacherCompetenceMatrixRepository matrixRepository) {
        return args -> {
            /* Handled by Flyway migration V2__Base_Template.sql */
            System.out.println(">>> Ініціалізація бази даних тепер виконується через Flyway.");
            System.out.println(">>> Слотов в базе: " + timeslotRepository.count());
            System.out.println(">>> Групп в базе: " + groupRepository.count());
            System.out.println(">>> Аудиторий в базе: " + roomRepository.count());
        };
    }
}
