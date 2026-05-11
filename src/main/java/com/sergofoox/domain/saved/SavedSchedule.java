package com.sergofoox.domain.saved;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
public class SavedSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean fullTemplate = false;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String snapshotJson;

    @OneToMany(mappedBy = "savedSchedule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SavedScheduleLesson> lessons = new ArrayList<>();

    @Column(nullable = false)
    private boolean autosaveEnabled = true;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isFullTemplate() {
        return fullTemplate;
    }

    public void setFullTemplate(boolean fullTemplate) {
        this.fullTemplate = fullTemplate;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public List<SavedScheduleLesson> getLessons() {
        return lessons;
    }

    public void setLessons(List<SavedScheduleLesson> lessons) {
        this.lessons = lessons;
    }

    public void replaceLessons(Collection<SavedScheduleLesson> newLessons) {
        lessons.clear();
        for (SavedScheduleLesson lesson : newLessons) {
            lesson.setSavedSchedule(this);
            lessons.add(lesson);
        }
    }

    public boolean isAutosaveEnabled() {
        return autosaveEnabled;
    }

    public void setAutosaveEnabled(boolean autosaveEnabled) {
        this.autosaveEnabled = autosaveEnabled;
    }
}
