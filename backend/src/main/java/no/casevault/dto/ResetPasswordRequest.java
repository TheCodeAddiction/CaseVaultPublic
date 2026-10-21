package no.casevault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank String username,
    @NotBlank String code,
    @Size(min = 8, max = 100) String newPassword
) {
}
