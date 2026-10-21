package no.casevault.dto;

import no.casevault.model.Conversation;
import no.casevault.model.Message;
import no.casevault.model.User;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummaryResponse(
    UUID id,
    String otherParticipantUsername,
    String otherParticipantFullName,
    String lastMessageBody,
    Instant lastMessageSentAt,
    String lastMessageSenderUsername,
    long unreadCount
) {
    public static ConversationSummaryResponse of(Conversation conversation, User self, Message lastMessage, long unreadCount) {
        User other = conversation.otherParticipant(self);

        return new ConversationSummaryResponse(
            conversation.getId(),
            other.getUsername(),
            other.getFullName(),
            lastMessage.getBody(),
            lastMessage.getSentAt(),
            lastMessage.getSender().getUsername(),
            unreadCount
        );
    }
}
