package no.casevault.controller;

import no.casevault.dto.DocumentResponse;
import no.casevault.dto.MessageResponse;
import no.casevault.exception.CaseNotFoundException;
import no.casevault.exception.InvalidCredentialsException;
import no.casevault.model.Case;
import no.casevault.model.Document;
import no.casevault.model.DocumentType;
import no.casevault.model.User;
import no.casevault.repository.UserRepository;
import no.casevault.service.AuditLogService;
import no.casevault.service.CaseService;
import no.casevault.service.DocumentService;
import no.casevault.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases/{caseId}/documents")
@PreAuthorize("hasAnyRole('OFFICER','SUPERVISOR','ADMIN')")
public class DocumentController {

    private static final String ENTITY_TYPE = "CASE";

    private final CaseService caseService;
    private final DocumentService documentService;
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public DocumentController(CaseService caseService, DocumentService documentService, AuditLogService auditLogService,
                               FileStorageService fileStorageService, UserRepository userRepository) {
        this.caseService = caseService;
        this.documentService = documentService;
        this.auditLogService = auditLogService;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<DocumentResponse> listDocuments(@PathVariable UUID caseId, Authentication authentication) {
        User user = currentUser(authentication);
        requireVisibleCase(user, caseId);

        return documentService.listFor(caseId).stream()
            .map(DocumentResponse::from)
            .toList();
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponse> addDocument(
        @PathVariable UUID caseId,
        @RequestParam DocumentType type,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String content,
        @RequestParam(required = false) MultipartFile file,
        Authentication authentication
    ) {
        User user = currentUser(authentication);
        Case caseRecord = requireVisibleCase(user, caseId);

        Document document = documentService.create(caseRecord, user, type, title, content, file);

        auditLogService.record(user, ENTITY_TYPE, caseId.toString(), activityAction(type),
            "Added a " + noun(type) + (title != null ? ": " + title : ""));

        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(document));
    }

    @DeleteMapping("/{documentId}")
    public MessageResponse deleteDocument(@PathVariable UUID caseId, @PathVariable Long documentId, Authentication authentication) {
        User user = currentUser(authentication);
        Case caseRecord = requireVisibleCase(user, caseId);
        Document document = documentService.findInCase(caseRecord, documentId);

        documentService.delete(caseRecord, document, user);

        return new MessageResponse("Document deleted.");
    }

    @GetMapping("/{documentId}/file")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID caseId, @PathVariable Long documentId, Authentication authentication) {
        User user = currentUser(authentication);
        requireVisibleCase(user, caseId);

        Document document = documentService.listFor(caseId).stream()
            .filter(d -> d.getId().equals(documentId) && d.getType() == DocumentType.FILE)
            .findFirst()
            .orElseThrow(() -> new CaseNotFoundException(documentId));

        Resource resource = fileStorageService.load(document.getStoredFilename());

        return ResponseEntity.ok()
            .contentType(org.springframework.http.MediaType.parseMediaType(document.getFileContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFilename() + "\"")
            .body(resource);
    }

    private String activityAction(DocumentType type) {
        return switch (type) {
            case MESSAGE -> "COMMENT_ADDED";
            case REPORT -> "REPORT_ADDED";
            case FILE -> "FILE_ADDED";
        };
    }

    private String noun(DocumentType type) {
        return switch (type) {
            case MESSAGE -> "comment";
            case REPORT -> "report";
            case FILE -> "file";
        };
    }

    private Case requireVisibleCase(User user, UUID caseId) {
        return caseService.findVisibleCase(user, caseId).orElseThrow(() -> new CaseNotFoundException(caseId));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
            .orElseThrow(InvalidCredentialsException::new);
    }
}
