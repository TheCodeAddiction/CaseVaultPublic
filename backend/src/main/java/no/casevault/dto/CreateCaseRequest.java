package no.casevault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCaseRequest(
    @Size(min = 3, max = 200) String title,
    @NotBlank @Size(max = 2000) String description
) {
}
