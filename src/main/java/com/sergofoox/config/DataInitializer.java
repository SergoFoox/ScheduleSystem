package com.sergofoox.config;

import com.sergofoox.domain.group.Group;
import com.sergofoox.domain.group.GroupRepository;
import com.sergofoox.domain.plan.RoomType;
import com.sergofoox.domain.room.Room;
import com.sergofoox.domain.room.RoomRepository;
import com.sergofoox.domain.teacher.PositionType;
import com.sergofoox.domain.teacher.Teacher;
import com.sergofoox.domain.teacher.TeacherRepository;
import com.sergofoox.domain.timeslot.Timeslot;
import com.sergofoox.domain.timeslot.TimeslotRepository;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.lesson.LessonRepository;
import com.sergofoox.domain.subject.Subject;
import com.sergofoox.domain.subject.SubjectRepository;
import com.sergofoox.domain.subject.LessonType;
import com.sergofoox.domain.plan.CoursePlan;
import com.sergofoox.domain.plan.CoursePlanRepository;
import com.sergofoox.domain.competence.TeacherCompetenceMatrix;
import com.sergofoox.domain.competence.TeacherCompetenceMatrixRepository;
import com.sergofoox.domain.competence.Priority;
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
            if (teacherRepository.count() > 0) return;

            // 1. Вчителі
            Teacher t1 = new Teacher("Іваненко Іван Петрович", "Кафедра ІТ", PositionType.FULL_TIME);
            t1.setSpecialization("Програмування");
            t1.setWeeklyHourLimit(36);
            t1.setMaxWorkingDaysPerWeek(5);
            t1 = teacherRepository.save(t1);

            Teacher t2 = new Teacher("Петренко Ольга Сидорівна", "Кафедра Математики", PositionType.PART_TIME);
            t2.setSpecialization("Вища математика");
            t2.setWeeklyHourLimit(18);
            t2.setMaxWorkingDaysPerWeek(3);
            t2 = teacherRepository.save(t2);

            // 2. Групи
            Group g1 = groupRepository.save(new Group("ТЕ-11", 25, 1, "Кафедра Т", t1.getId()));
            Group g2 = groupRepository.save(new Group("ВГ-11", 20, 1, "Кафедра ВГ", t2.getId()));

            // 3. Аудиторії
            roomRepository.save(new Room("507", Integer.valueOf(30), "Корпус 1", "ПК", RoomType.COMPUTER_CLASS));
            roomRepository.save(new Room("26", Integer.valueOf(30), "Корпус 1", "Проектор", RoomType.LECTURE_HALL));

            // 4. Таймслоти
            timeslotRepository.save(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1));
            timeslotRepository.save(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(10, 15), LocalTime.of(11, 45), 2));
            timeslotRepository.save(new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), 1));
            timeslotRepository.save(new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(10, 15), LocalTime.of(11, 45), 2));

            // 5. Предмети
            Subject s1 = subjectRepository.save(new Subject("Інформатика", "ІНФ"));
            Subject s2 = subjectRepository.save(new Subject("Українська мова", "УКР"));

            // 6. Матриця компетенцій
            matrixRepository.save(new TeacherCompetenceMatrix(t1, s1, LessonType.LABORATORY, Priority.PRIMARY));
            matrixRepository.save(new TeacherCompetenceMatrix(t1, s1, LessonType.LECTURE, Priority.SECONDARY));
            matrixRepository.save(new TeacherCompetenceMatrix(t2, s2, LessonType.LECTURE, Priority.PRIMARY));

            // 7. Плани
            CoursePlan cp1 = coursePlanRepository.save(new CoursePlan(s1, g1, 32, 16, 16, 0, 1, 1, 0, RoomType.COMPUTER_CLASS));
            CoursePlan cp2 = coursePlanRepository.save(new CoursePlan(s2, g2, 32, 16, 16, 0, 1, 1, 0, RoomType.GENERAL_CLASSROOM));

            // 8. Заняття (Lesson) - створюємо "заготовки", які солвер буде розставляти
            lessonRepository.save(new Lesson(s1, LessonType.LABORATORY, t1, g1, cp1, 0));
            lessonRepository.save(new Lesson(s2, LessonType.LECTURE, t2, g2, cp2, 0));
        };
    }
}
