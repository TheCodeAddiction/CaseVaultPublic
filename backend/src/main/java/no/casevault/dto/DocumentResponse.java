package no.casevault.dto;

import no.casevault.model.Document;
import no.casevault.model.DocumentType;

import java.time.Instant;

public record DocumentResponse(
    Long id,
    DocumentType type,
    String title,
    String content,
    String originalFilename,
    String fileContentType,
    Long fileSizeBytes,
    String authorUsername,
    String authorFullName,
    Instant createdAt
) {
    public static DocumentResponse from(Document d) {
        return new DocumentResponse(
            d.getId(), d.getType(), d.getTitle(), d.getContent(),
            d.getOriginalFilename(), d.getFileContentType(), d.getFileSizeBytes(),
            d.getAuthor().getUsername(), d.getAuthor().getFullName(),
            d.getCreatedAt()
        );
    }
}
