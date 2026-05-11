package com.sergofoox.domain.ui;

import org.springframework.stereotype.Service;

@Service
public class TemplateAccessService {

    public static final String BASE_TEMPLATE_EDIT_MESSAGE = "Для редагування потрібно створити новий розклад або скопіювати існуючий шаблон.";

    private volatile boolean baseTemplateLocked = true;
    private volatile boolean baseTemplateOpened = false;
    private volatile Long activeSavedScheduleId;

    public boolean isBaseTemplateLocked() {
        return baseTemplateLocked;
    }

    public boolean isBaseTemplateOpened() {
        return baseTemplateOpened;
    }

    public Long getActiveSavedScheduleId() {
        return activeSavedScheduleId;
    }

    public void lockBaseTemplate() {
        baseTemplateLocked = true;
        baseTemplateOpened = true;
        activeSavedScheduleId = null;
    }

    public void resetBaseTemplateSession() {
        baseTemplateLocked = true;
        baseTemplateOpened = false;
        activeSavedScheduleId = null;
    }

    public void activateEditableTemplate(Long savedScheduleId) {
        baseTemplateLocked = false;
        baseTemplateOpened = false;
        activeSavedScheduleId = savedScheduleId;
    }

    public void requireWritableTemplate() {
        if (baseTemplateLocked) {
            throw new IllegalStateException(BASE_TEMPLATE_EDIT_MESSAGE);
        }
    }
}
