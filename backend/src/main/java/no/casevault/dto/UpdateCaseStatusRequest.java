package no.casevault.dto;

import jakarta.validation.constraints.NotNull;
import no.casevault.model.CaseStatus;

public record UpdateCaseStatusRequest(
    @NotNull CaseStatus status
) {
}
