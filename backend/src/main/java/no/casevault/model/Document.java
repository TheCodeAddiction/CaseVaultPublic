package no.casevault.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseRecord;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType type;

    private String title;

    @Column(length = 10000)
    private String content;

    private String storedFilename;

    private String originalFilename;

    private String fileContentType;

    private Long fileSizeBytes;

    @Column(nullable = false)
    private Instant createdAt;

    protected Document() {
    }

    public static Document textDocument(Case caseRecord, User author, DocumentType type, String title, String content, Instant createdAt) {
        Document document = new Document();
        document.caseRecord = caseRecord;
        document.author = author;
        document.type = type;
        document.title = title;
        document.content = content;
        document.createdAt = createdAt;
        return document;
    }

    public static Document fileDocument(Case caseRecord, User author, String title, String storedFilename,
                                         String originalFilename, String fileContentType, long fileSizeBytes, Instant createdAt) {
        Document document = new Document();
        document.caseRecord = caseRecord;
        document.author = author;
        document.type = DocumentType.FILE;
        document.title = title;
        document.storedFilename = storedFilename;
        document.originalFilename = originalFilename;
        document.fileContentType = fileContentType;
        document.fileSizeBytes = fileSizeBytes;
        document.createdAt = createdAt;
        return document;
    }

    public Long getId() {
        return id;
    }

    public Case getCaseRecord() {
        return caseRecord;
    }

    public User getAuthor() {
        return author;
    }

    public DocumentType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getFileContentType() {
        return fileContentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
