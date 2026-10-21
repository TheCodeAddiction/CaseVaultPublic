package no.casevault.dto;

import no.casevault.model.Case;
import no.casevault.model.CaseStatus;
import no.casevault.model.District;

import java.time.Instant;
import java.util.UUID;

public record CaseDetailResponse(
    UUID id,
    String caseNumber,
    String title,
    String description,
    CaseStatus status,
    District district,
    String assignedOfficerUsername,
    Instant createdAt
) {
    public static CaseDetailResponse from(Case c) {
        return new CaseDetailResponse(
            c.getId(), c.getCaseNumber(), c.getTitle(), c.getDescription(), c.getStatus(),
            c.getDistrict(),
            c.getAssignedOfficer() != null ? c.getAssignedOfficer().getUsername() : null,
            c.getCreatedAt()
        );
    }
}
