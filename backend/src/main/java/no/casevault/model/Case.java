package no.casevault.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cases")
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String caseNumber;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private District district;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_officer_id")
    private User assignedOfficer;

    @Column(nullable = false)
    private Instant createdAt;

    protected Case() {
    }

    public Case(String caseNumber, String title, String description, CaseStatus status,
                District district, User assignedOfficer, Instant createdAt) {
        this.caseNumber = caseNumber;
        this.title = title;
        this.description = description;
        this.status = status;
        this.district = district;
        this.assignedOfficer = assignedOfficer;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public District getDistrict() {
        return district;
    }

    public User getAssignedOfficer() {
        return assignedOfficer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void changeStatus(CaseStatus newStatus) {
        this.status = newStatus;
    }

    public void assignOfficer(User officer) {
        this.assignedOfficer = officer;
    }
}
