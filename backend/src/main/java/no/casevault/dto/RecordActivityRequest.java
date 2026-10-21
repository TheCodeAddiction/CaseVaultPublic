package no.casevault.dto;

import jakarta.validation.constraints.NotBlank;

public record RecordActivityRequest(
    @NotBlank String entityType,
    @NotBlank String entityId,
    @NotBlank String action,
    String details
) {
}
