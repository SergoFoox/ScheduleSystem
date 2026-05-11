package com.sergofoox.domain.autosave;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import com.sergofoox.domain.ui.TemplateAccessService;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@BrowserCallable
@Service
@AnonymousAllowed
public class AutosaveEndpoint {

    private final AutosaveService autosaveService;
    private final AutosaveRepository autosaveRepository;
    private final TemplateAccessService templateAccessService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public AutosaveEndpoint(AutosaveService autosaveService, 
                            AutosaveRepository autosaveRepository,
                            TemplateAccessService templateAccessService) {
        this.autosaveService = autosaveService;
        this.autosaveRepository = autosaveRepository;
        this.templateAccessService = templateAccessService;
    }

    /**
     * Returns a list of the latest snapshots for the currently active schedule.
     */
    public List<AutosaveSnapshotDTO> getLatestSnapshots() {
        Long activeId = templateAccessService.getActiveSavedScheduleId();
        if (activeId == null) {
            return Collections.emptyList();
        }

        return autosaveRepository.findByScheduleIdOrderByTimestampDesc(activeId).stream()
                .map(snapshot -> new AutosaveSnapshotDTO(
                        snapshot.getId(),
                        snapshot.getTimestamp().format(FORMATTER),
                        snapshot.getEntityCount(),
                        snapshot.isManual()
                ))
                .toList();
    }

    /**
     * Captures a manual snapshot of the system state.
     */
    public void captureManualSnapshot() {
        autosaveService.captureSnapshot(true);
    }

    /**
     * Deletes a specific snapshot by ID.
     * @param id the ID of the snapshot to delete.
     */
    public void deleteSnapshot(Long id) {
        autosaveService.deleteSnapshot(id);
    }

    /**
     * Restores the system state from a specific snapshot.
     * @param id the ID of the snapshot to restore.
     * @param asNewTemplate if true, restores as a new saved schedule; if false, performs a full rollback.
     */
    public void restoreSnapshot(Long id, boolean asNewTemplate) {
        autosaveService.restoreSnapshot(id, asNewTemplate);
    }
}
