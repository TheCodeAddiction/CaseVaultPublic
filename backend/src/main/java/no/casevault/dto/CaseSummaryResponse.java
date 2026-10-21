package no.casevault.dto;

import no.casevault.model.Case;
import no.casevault.model.CaseStatus;
import no.casevault.model.District;

import java.time.Instant;
import java.util.UUID;

public record CaseSummaryResponse(
    UUID id,
    String caseNumber,
    String title,
    CaseStatus status,
    District district,
    String assignedOfficerUsername,
    Instant createdAt
) {
    public static CaseSummaryResponse from(Case c) {
        return new CaseSummaryResponse(
            c.getId(), c.getCaseNumber(), c.getTitle(), c.getStatus(),
            c.getDistrict(),
            c.getAssignedOfficer() != null ? c.getAssignedOfficer().getUsername() : null,
            c.getCreatedAt()
        );
    }
}
