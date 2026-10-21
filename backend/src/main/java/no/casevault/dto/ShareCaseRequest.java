package no.casevault.dto;

import no.casevault.model.District;

public record ShareCaseRequest(
    District district,
    String username
) {
}
