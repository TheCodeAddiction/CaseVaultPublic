package no.casevault.service;

import no.casevault.exception.DocumentNotFoundException;
import no.casevault.exception.InvalidDocumentException;
import no.casevault.model.Case;
import no.casevault.model.Document;
import no.casevault.model.DocumentType;
import no.casevault.model.Role;
import no.casevault.model.User;
import no.casevault.repository.DocumentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    public DocumentService(DocumentRepository documentRepository, FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
    }

    public Document create(Case caseRecord, User author, DocumentType type, String title, String content, MultipartFile file) {
        Instant now = Instant.now();

        if (type == DocumentType.FILE) {
            if (file == null || file.isEmpty()) {
                throw new InvalidDocumentException("A file is required for document type FILE");
            }
            StoredFile stored = storeFile(file);
            Document document = Document.fileDocument(
                caseRecord, author, title, stored.storedFilename(), stored.originalFilename(),
                stored.contentType(), stored.sizeBytes(), now
            );
            return documentRepository.save(document);
        }

        if (!StringUtils.hasText(content)) {
            throw new InvalidDocumentException("Content is required for document type " + type);
        }
        return documentRepository.save(Document.textDocument(caseRecord, author, type, title, content, now));
    }

    public List<Document> listFor(UUID caseId) {
        return documentRepository.findByCaseRecordIdOrderByCreatedAtAsc(caseId);
    }

    public Document findInCase(Case caseRecord, Long documentId) {
        return documentRepository.findById(documentId)
            .filter(d -> d.getCaseRecord().getId().equals(caseRecord.getId()))
            .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    public void delete(Case caseRecord, Document document, User actor) {
        if (!canDelete(actor, caseRecord, document)) {
            throw new AccessDeniedException("Not authorized to delete this document");
        }
        documentRepository.delete(document);
    }

    private boolean canDelete(User actor, Case caseRecord, Document document) {
        if (actor.getRole() == Role.ADMIN) {
            return true;
        }
        if (actor.getRole() == Role.SUPERVISOR && actor.getDistrict().equals(caseRecord.getDistrict())) {
            return true;
        }
        return document.getAuthor().getId().equals(actor.getId());
    }

    private StoredFile storeFile(MultipartFile file) {
        try {
            return fileStorageService.store(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
