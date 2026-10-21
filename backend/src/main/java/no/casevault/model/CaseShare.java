package no.casevault.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "case_shares")
public class CaseShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "shared_with_district")
    private District sharedWithDistrict;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shared_with_user_id")
    private User sharedWithUser;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "shared_by_id", nullable = false)
    private User sharedBy;

    @Column(nullable = false)
    private Instant sharedAt;

    protected CaseShare() {
    }

    public static CaseShare withDistrict(Case caseRecord, District district, User sharedBy, Instant sharedAt) {
        CaseShare share = new CaseShare();
        share.caseRecord = caseRecord;
        share.sharedWithDistrict = district;
        share.sharedBy = sharedBy;
        share.sharedAt = sharedAt;
        return share;
    }

    public static CaseShare withUser(Case caseRecord, User sharedWithUser, User sharedBy, Instant sharedAt) {
        CaseShare share = new CaseShare();
        share.caseRecord = caseRecord;
        share.sharedWithUser = sharedWithUser;
        share.sharedBy = sharedBy;
        share.sharedAt = sharedAt;
        return share;
    }

    public Long getId() {
        return id;
    }

    public Case getCaseRecord() {
        return caseRecord;
    }

    public District getSharedWithDistrict() {
        return sharedWithDistrict;
    }

    public User getSharedWithUser() {
        return sharedWithUser;
    }

    public User getSharedBy() {
        return sharedBy;
    }

    public Instant getSharedAt() {
        return sharedAt;
    }
}
