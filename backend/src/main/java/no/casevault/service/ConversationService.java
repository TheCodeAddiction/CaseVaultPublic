package no.casevault.service;

import no.casevault.dto.ConversationSummaryResponse;
import no.casevault.exception.ConversationNotFoundException;
import no.casevault.model.Conversation;
import no.casevault.model.User;
import no.casevault.repository.ConversationRepository;
import no.casevault.repository.MessageRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public Conversation findOrCreate(User a, User b) {
        User first = a.getId() <= b.getId() ? a : b;
        User second = first.getId().equals(a.getId()) ? b : a;

        return conversationRepository.findByUserAAndUserB(first, second)
            .orElseGet(() -> createConversation(first, second));
    }

    private Conversation createConversation(User first, User second) {
        try {
            return conversationRepository.save(new Conversation(first, second, Instant.now()));
        } catch (DataIntegrityViolationException e) {
            return conversationRepository.findByUserAAndUserB(first, second).orElseThrow(() -> e);
        }
    }

    public Conversation findForParticipant(User user, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        if (!conversation.hasParticipant(user)) {
            throw new AccessDeniedException("Only participants may access this conversation");
        }

        return conversation;
    }

    public List<ConversationSummaryResponse> listSummariesForUser(User user) {
        return conversationRepository.findAllForUser(user).stream()
            .flatMap(conversation -> messageRepository.findTopByConversationOrderBySentAtDesc(conversation)
                .map(lastMessage -> {
                    long unreadCount = messageRepository.countByConversationAndRecipientAndReadAtIsNull(conversation, user);
                    return ConversationSummaryResponse.of(conversation, user, lastMessage, unreadCount);
                })
                .stream())
            .sorted(Comparator.comparing(ConversationSummaryResponse::lastMessageSentAt).reversed())
            .toList();
    }
}
