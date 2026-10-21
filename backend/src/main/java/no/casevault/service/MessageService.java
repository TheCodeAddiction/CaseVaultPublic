package no.casevault.service;

import no.casevault.exception.CaseNotFoundException;
import no.casevault.exception.InvalidMessageException;
import no.casevault.exception.MessageNotFoundException;
import no.casevault.exception.UserNotFoundException;
import no.casevault.model.Case;
import no.casevault.model.Conversation;
import no.casevault.model.Message;
import no.casevault.model.User;
import no.casevault.repository.MessageRepository;
import no.casevault.repository.UserRepository;
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
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CaseService caseService;
    private final FileStorageService fileStorageService;
    private final ConversationService conversationService;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository,
                           CaseService caseService, FileStorageService fileStorageService,
                           ConversationService conversationService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.caseService = caseService;
        this.fileStorageService = fileStorageService;
        this.conversationService = conversationService;
    }

    private static final int MAX_BODY_LENGTH = 4000;

    public Message sendNewMessage(User sender, String recipientUsername, String body, UUID relatedCaseId, MultipartFile attachment) {
        validateBody(body);

        User recipient = userRepository.findByUsernameIgnoreCase(recipientUsername)
            .orElseThrow(() -> new UserNotFoundException(recipientUsername));

        if (recipient.getId().equals(sender.getId())) {
            throw new InvalidMessageException("Cannot send a message to yourself");
        }

        Message message = buildMessage(sender, recipient, body, relatedCaseId, attachment);
        Conversation conversation = conversationService.findOrCreate(sender, recipient);
        message.assignConversation(conversation);
        return messageRepository.save(message);
    }

    private void validateBody(String body) {
        if (!StringUtils.hasText(body)) {
            throw new InvalidMessageException("Message body is required");
        }
        if (body.length() > MAX_BODY_LENGTH) {
            throw new InvalidMessageException("Message body must be " + MAX_BODY_LENGTH + " characters or fewer");
        }
    }

    private Message buildMessage(User sender, User recipient, String body, UUID relatedCaseId, MultipartFile attachment) {
        Message message = new Message(sender, recipient, body, Instant.now());

        if (relatedCaseId != null) {
            Case relatedCase = caseService.findVisibleCase(sender, relatedCaseId)
                .orElseThrow(() -> new CaseNotFoundException(relatedCaseId));
            message.linkCase(relatedCase);
        }

        if (attachment != null && !attachment.isEmpty()) {
            StoredFile stored = storeFile(attachment);
            message.attachFile(stored.storedFilename(), stored.originalFilename(), stored.contentType());
        }

        return message;
    }

    public Message sendToConversation(User sender, UUID conversationId, String body, UUID relatedCaseId, MultipartFile attachment) {
        validateBody(body);

        Conversation conversation = conversationService.findForParticipant(sender, conversationId);
        User recipient = conversation.otherParticipant(sender);
        Message message = buildMessage(sender, recipient, body, relatedCaseId, attachment);
        message.assignConversation(conversation);
        return messageRepository.save(message);
    }

    public List<Message> listMessages(User user, UUID conversationId) {
        Conversation conversation = conversationService.findForParticipant(user, conversationId);
        return messageRepository.findByConversationOrderBySentAtAsc(conversation);
    }

    public List<Message> markConversationRead(User user, UUID conversationId) {
        Conversation conversation = conversationService.findForParticipant(user, conversationId);
        List<Message> unread = messageRepository.findByConversationAndRecipientAndReadAtIsNull(conversation, user);
        Instant now = Instant.now();
        unread.forEach(m -> m.markRead(now));
        return messageRepository.saveAll(unread);
    }

    public long unreadCount(User user) {
        return messageRepository.countByRecipientAndReadAtIsNull(user);
    }

    public Message findForParticipant(User user, UUID messageId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new MessageNotFoundException(messageId));

        boolean isParticipant = message.getSender().getId().equals(user.getId())
            || message.getRecipient().getId().equals(user.getId());
        if (!isParticipant) {
            throw new AccessDeniedException("Only the sender or recipient can access this message");
        }

        return message;
    }

    private StoredFile storeFile(MultipartFile file) {
        try {
            return fileStorageService.store(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
