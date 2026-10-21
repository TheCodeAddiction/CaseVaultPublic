package no.casevault.service;

import no.casevault.model.ActivityLogEntry;
import no.casevault.model.User;
import no.casevault.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuditLogService {

    private final ActivityLogRepository activityLogRepository;

    public AuditLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void record(User actor, String entityType, String entityId, String action, String details) {
        activityLogRepository.save(new ActivityLogEntry(actor, entityType, entityId, action, details, Instant.now()));
    }

    public List<ActivityLogEntry> findFor(String entityType, String entityId) {
        return activityLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(entityType, entityId);
    }
}
