package no.casevault.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private Instant sentAt;

    private Instant readAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "related_case_id")
    private Case relatedCase;

    private String attachmentStoredFilename;

    private String attachmentOriginalFilename;

    private String attachmentContentType;

    protected Message() {
    }

    public Message(User sender, User recipient, String body, Instant sentAt) {
        this.sender = sender;
        this.recipient = recipient;
        this.body = body;
        this.sentAt = sentAt;
    }

    public UUID getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void assignConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public User getSender() {
        return sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public String getBody() {
        return body;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void markRead(Instant readAt) {
        this.readAt = readAt;
    }

    public Case getRelatedCase() {
        return relatedCase;
    }

    public void linkCase(Case relatedCase) {
        this.relatedCase = relatedCase;
    }

    public String getAttachmentStoredFilename() {
        return attachmentStoredFilename;
    }

    public String getAttachmentOriginalFilename() {
        return attachmentOriginalFilename;
    }

    public String getAttachmentContentType() {
        return attachmentContentType;
    }

    public void attachFile(String storedFilename, String originalFilename, String contentType) {
        this.attachmentStoredFilename = storedFilename;
        this.attachmentOriginalFilename = originalFilename;
        this.attachmentContentType = contentType;
    }
}
