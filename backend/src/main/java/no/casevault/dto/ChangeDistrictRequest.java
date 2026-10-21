package no.casevault.dto;

import jakarta.validation.constraints.NotNull;
import no.casevault.model.District;

public record ChangeDistrictRequest(@NotNull District district) {
}
