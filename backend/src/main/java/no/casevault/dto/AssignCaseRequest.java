package no.casevault.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignCaseRequest(
    @NotBlank String officerUsername
) {
}
