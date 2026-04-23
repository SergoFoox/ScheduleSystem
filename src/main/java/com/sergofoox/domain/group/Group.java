package com.sergofoox.domain.group;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Objects;

@Entity
@Table(name = "student_group") // 'group' is a reserved keyword in SQL
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer size;

    @NotNull
    @Min(1)
    @Max(4)
    @Column(nullable = false)
    private Integer course;

    @NotBlank
    @Column(nullable = false)
    private String department;

    private Long curatorId;

    public Group() {}

    public Group(String name, Integer size, Integer course, String department) {
        this.name = name;
        this.size = size;
        this.course = course;
        this.department = department;
    }

    public Group(String name, Integer size, Integer course, String department, Long curatorId) {
        this.name = name;
        this.size = size;
        this.course = course;
        this.department = department;
        this.curatorId = curatorId;
    }

    public Group(Long id, String name, Integer size, Integer course, String department) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.course = course;
        this.department = department;
    }

    public Group(Long id, String name, Integer size, Integer course, String department, Long curatorId) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.course = course;
        this.department = department;
        this.curatorId = curatorId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public Integer getCourse() { return course; }
    public void setCourse(Integer course) { this.course = course; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Long getCuratorId() { return curatorId; }
    public void setCuratorId(Long curatorId) { this.curatorId = curatorId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group group)) return false;
        return Objects.equals(name, group.name) &&
               Objects.equals(department, group.department);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, department);
    }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", course=" + course +
                ", department='" + department + '\'' +
                ", curatorId=" + curatorId +
                '}';
    }
}
