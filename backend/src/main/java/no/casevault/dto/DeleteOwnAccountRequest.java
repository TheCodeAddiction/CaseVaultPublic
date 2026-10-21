package no.casevault.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteOwnAccountRequest(@NotBlank String currentPassword) {
}
