package no.casevault.dto;

import no.casevault.model.CaseShare;
import no.casevault.model.District;

import java.time.Instant;

public record CaseShareResponse(
    Long id,
    District sharedWithDistrict,
    String sharedWithUsername,
    String sharedByUsername,
    Instant sharedAt
) {
    public static CaseShareResponse from(CaseShare share) {
        return new CaseShareResponse(
            share.getId(),
            share.getSharedWithDistrict(),
            share.getSharedWithUser() != null ? share.getSharedWithUser().getUsername() : null,
            share.getSharedBy().getUsername(),
            share.getSharedAt()
        );
    }
}
