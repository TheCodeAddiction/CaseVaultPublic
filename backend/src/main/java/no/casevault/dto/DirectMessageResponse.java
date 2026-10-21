package no.casevault.dto;

import no.casevault.model.Message;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageResponse(
    UUID id,
    UUID conversationId,
    String senderUsername,
    String senderFullName,
    String recipientUsername,
    String recipientFullName,
    String body,
    Instant sentAt,
    Instant readAt,
    UUID relatedCaseId,
    String relatedCaseNumber,
    String relatedCaseTitle,
    String attachmentFilename
) {
    public static DirectMessageResponse from(Message message) {
        var relatedCase = message.getRelatedCase();

        return new DirectMessageResponse(
            message.getId(),
            message.getConversation().getId(),
            message.getSender().getUsername(),
            message.getSender().getFullName(),
            message.getRecipient().getUsername(),
            message.getRecipient().getFullName(),
            message.getBody(),
            message.getSentAt(),
            message.getReadAt(),
            relatedCase != null ? relatedCase.getId() : null,
            relatedCase != null ? relatedCase.getCaseNumber() : null,
            relatedCase != null ? relatedCase.getTitle() : null,
            message.getAttachmentOriginalFilename()
        );
    }
}
