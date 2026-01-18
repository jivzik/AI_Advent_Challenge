package de.jivz.agentservice.cli.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

/**
 * Shared state service for CLI operations.
 * Stores generated release notes to be used by CreateReleaseExecutor.
 */
@Service
@Getter
@Setter
public class CLIStateService {

    private String lastGeneratedReleaseNotes;

    public void saveReleaseNotes(String notes) {
        this.lastGeneratedReleaseNotes = notes;
    }

    public String getAndClearReleaseNotes() {
        String notes = this.lastGeneratedReleaseNotes;
        this.lastGeneratedReleaseNotes = null;
        return notes;
    }

    public boolean hasReleaseNotes() {
        return lastGeneratedReleaseNotes != null && !lastGeneratedReleaseNotes.isEmpty();
    }
}

