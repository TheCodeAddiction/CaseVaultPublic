package no.casevault.dto;

import jakarta.validation.constraints.NotNull;
import no.casevault.model.Role;

public record ChangeRoleRequest(@NotNull Role role) {
}
