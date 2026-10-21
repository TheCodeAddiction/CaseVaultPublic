package no.casevault.dto;

import java.util.List;

public record CasePageResponse(
    List<CaseSummaryResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
