package no.casevault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.casevault.model.District;
import no.casevault.model.Role;

public record CreateUserRequest(
    @Size(min = 2, max = 100) String fullName,
    @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}") String username,
    @NotBlank @Email String email,
    @Size(min = 8, max = 100) String password,
    @NotNull Role role,
    @NotNull District district
) {
}
