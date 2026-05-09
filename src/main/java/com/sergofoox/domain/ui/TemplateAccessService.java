package com.sergofoox.domain.ui;

import org.springframework.stereotype.Service;

@Service
public class TemplateAccessService {

    public static final String BASE_TEMPLATE_EDIT_MESSAGE = "Для цього потрібно скопіювати шаблон.";

    private volatile boolean baseTemplateLocked = true;
    private volatile Long activeSavedScheduleId;

    public boolean isBaseTemplateLocked() {
        return baseTemplateLocked;
    }

    public Long getActiveSavedScheduleId() {
        return activeSavedScheduleId;
    }

    public void lockBaseTemplate() {
        baseTemplateLocked = true;
        activeSavedScheduleId = null;
    }

    public void activateEditableTemplate(Long savedScheduleId) {
        baseTemplateLocked = false;
        activeSavedScheduleId = savedScheduleId;
    }

    public void requireWritableTemplate() {
        if (baseTemplateLocked) {
            throw new IllegalStateException(BASE_TEMPLATE_EDIT_MESSAGE);
        }
    }
}
