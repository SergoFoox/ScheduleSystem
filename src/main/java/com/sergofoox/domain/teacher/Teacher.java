package com.sergofoox.domain.teacher;

import com.sergofoox.domain.room.Room;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

@Entity
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Column(nullable = false)
    private String fullName;

    @NotBlank(message = "Department is required")
    @Column(nullable = false)
    private String department;

    private String specialization;

    @NotNull(message = "Position type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PositionType positionType;

    @NotNull
    @Min(0)
    private Integer weeklyHourLimit = 40; // Default or common limit

    @Min(1)
    @Max(6)
    private Integer maxWorkingDaysPerWeek;

    @ManyToOne(fetch = FetchType.LAZY)
    private Room assignedRoom;

    private boolean archived = false;

    public Teacher() {}

    public Teacher(String fullName, String department, PositionType positionType) {
        this.fullName = fullName;
        this.department = department;
        this.positionType = positionType;
    }

    public Teacher(Long id, String fullName, String department, PositionType positionType, Integer weeklyHourLimit) {
        this.id = id;
        this.fullName = fullName;
        this.department = department;
        this.positionType = positionType;
        this.weeklyHourLimit = weeklyHourLimit;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public PositionType getPositionType() { return positionType; }
    public void setPositionType(PositionType positionType) { this.positionType = positionType; }
    public Integer getWeeklyHourLimit() { return weeklyHourLimit; }
    public void setWeeklyHourLimit(Integer weeklyHourLimit) { this.weeklyHourLimit = weeklyHourLimit; }
    public Integer getMaxWorkingDaysPerWeek() { return maxWorkingDaysPerWeek; }
    public void setMaxWorkingDaysPerWeek(Integer maxWorkingDaysPerWeek) { this.maxWorkingDaysPerWeek = maxWorkingDaysPerWeek; }
    public Room getAssignedRoom() { return assignedRoom; }
    public void setAssignedRoom(Room assignedRoom) { this.assignedRoom = assignedRoom; }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Teacher other)) return false;
        return Objects.equals(fullName, other.fullName) &&
               Objects.equals(department, other.department) &&
               Objects.equals(positionType, other.positionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName, department, positionType);
    }

    @Override
    public String toString() {
        return "Teacher{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", department='" + department + '\'' +
                ", positionType=" + positionType +
                ", weeklyHourLimit=" + weeklyHourLimit +
                ", maxWorkingDaysPerWeek=" + maxWorkingDaysPerWeek +
                '}';
    }
}
