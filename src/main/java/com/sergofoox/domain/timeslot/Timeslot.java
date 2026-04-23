package com.sergofoox.domain.timeslot;

import com.sergofoox.domain.plan.Periodicity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

@Entity
public class Timeslot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Periodicity weekParity = Periodicity.WEEKLY;

    @NotNull
    @Min(1)
    @Max(8)
    private Integer lessonNumber;

    public Timeslot() {
    }

    public Timeslot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, Integer lessonNumber) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lessonNumber = lessonNumber;
    }

    public Timeslot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, Periodicity weekParity, Integer lessonNumber) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.weekParity = weekParity;
        this.lessonNumber = lessonNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Periodicity getWeekParity() {
        return weekParity;
    }

    public void setWeekParity(Periodicity weekParity) {
        this.weekParity = weekParity;
    }

    public Integer getLessonNumber() {
        return lessonNumber;
    }

    public void setLessonNumber(Integer lessonNumber) {
        this.lessonNumber = lessonNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Timeslot timeslot)) return false;
        return dayOfWeek == timeslot.dayOfWeek && 
               Objects.equals(startTime, timeslot.startTime) && 
               Objects.equals(endTime, timeslot.endTime) && 
               weekParity == timeslot.weekParity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOfWeek, startTime, endTime, weekParity);
    }

    @Override
    public String toString() {
        return dayOfWeek + " " + startTime + "-" + endTime + " (" + weekParity + ")";
    }
}
