package com.sergofoox.domain.room;

import com.sergofoox.domain.plan.RoomType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotNull
    @Min(1)
    private Integer capacity;

    @NotBlank
    private String building;

    private String equipment;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RoomType type;

    public Room() {
    }

    public Room(String name, Integer capacity, String building, String equipment, RoomType type) {
        this.name = name;
        this.capacity = capacity;
        this.building = building;
        this.equipment = equipment;
        this.type = type;
    }

    public Room(Long id, String name, Integer capacity, String building, String equipment, RoomType type) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.building = building;
        this.equipment = equipment;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room other)) return false;
        if (id != null && other.id != null) return Objects.equals(id, other.id);
        return Objects.equals(name, other.name) && Objects.equals(building, other.building);
    }

    @Override
    public int hashCode() {
        if (id != null) return Objects.hash(id);
        return Objects.hash(name, building);
    }

    @Override
    public String toString() {
        return name + " (" + building + ")";
    }
}
