package no.casevault.dto;

import no.casevault.model.ActivityLogEntry;

import java.time.Instant;

public record ActivityLogEntryResponse(
    Long id,
    String actorUsername,
    String actorFullName,
    String action,
    String details,
    Instant occurredAt
) {
    public static ActivityLogEntryResponse from(ActivityLogEntry entry) {
        return new ActivityLogEntryResponse(
            entry.getId(),
            entry.getActor().getUsername(),
            entry.getActor().getFullName(),
            entry.getAction(),
            entry.getDetails(),
            entry.getOccurredAt()
        );
    }
}
