package no.casevault.repository;

import no.casevault.model.Document;
import no.casevault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByCaseRecordIdOrderByCreatedAtAsc(UUID caseId);

    boolean existsByAuthor(User author);
}
