package no.casevault.dto;

import java.util.List;

public record CaseStatsResponse(
    long totalCases,
    long openCases,
    long inProgressCases,
    long closedCases,
    List<CaseSummaryResponse> recentCases
) {
}
