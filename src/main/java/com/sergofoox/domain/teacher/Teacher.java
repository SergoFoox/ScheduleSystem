package com.sergofoox.domain.teacher;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String positionType;

    public Teacher() {}

    public Teacher(String fullName, String department, String positionType) {
        this.fullName = fullName;
        this.department = department;
        this.positionType = positionType;
    }

    public Teacher(Long id, String fullName, String department, String positionType) {
        this.id = id;
        this.fullName = fullName;
        this.department = department;
        this.positionType = positionType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getPositionType() { return positionType; }
    public void setPositionType(String positionType) { this.positionType = positionType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Teacher teacher = (Teacher) o;
        return java.util.Objects.equals(id, teacher.id) &&
               java.util.Objects.equals(fullName, teacher.fullName) &&
               java.util.Objects.equals(department, teacher.department) &&
               java.util.Objects.equals(positionType, teacher.positionType);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, fullName, department, positionType);
    }

    @Override
    public String toString() {
        return "Teacher{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", department='" + department + '\'' +
                ", positionType='" + positionType + '\'' +
                '}';
    }
}
