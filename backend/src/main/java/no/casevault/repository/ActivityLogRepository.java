package no.casevault.repository;

import no.casevault.model.ActivityLogEntry;
import no.casevault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLogEntry, Long> {

    List<ActivityLogEntry> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(String entityType, String entityId);

    boolean existsByActor(User actor);
}
