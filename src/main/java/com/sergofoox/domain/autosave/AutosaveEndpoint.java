package com.sergofoox.domain.autosave;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@BrowserCallable
@Service
@AnonymousAllowed
public class AutosaveEndpoint {

    private final AutosaveService autosaveService;
    private final AutosaveRepository autosaveRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public AutosaveEndpoint(AutosaveService autosaveService, AutosaveRepository autosaveRepository) {
        this.autosaveService = autosaveService;
        this.autosaveRepository = autosaveRepository;
    }

    /**
     * Retrieves the latest autosave snapshots from the database.
     * @return a list of snapshot DTOs.
     */
    public List<AutosaveSnapshotDTO> getLatestSnapshots() {
        return autosaveRepository.findAllByOrderByTimestampDesc().stream()
                .map(snapshot -> new AutosaveSnapshotDTO(
                        snapshot.getId(),
                        snapshot.getTimestamp().format(FORMATTER),
                        snapshot.getEntityCount()
                ))
                .toList();
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
