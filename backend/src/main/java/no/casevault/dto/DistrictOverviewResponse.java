package no.casevault.dto;

import no.casevault.model.District;

import java.util.List;

public record DistrictOverviewResponse(
    District district,
    long totalCases,
    long openCases,
    long inProgressCases,
    long closedCases,
    List<OfficerWorkloadResponse> officerWorkload
) {
}
