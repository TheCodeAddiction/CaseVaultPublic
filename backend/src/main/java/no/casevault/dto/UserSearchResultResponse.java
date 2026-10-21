package no.casevault.dto;

import no.casevault.model.District;
import no.casevault.model.Role;
import no.casevault.model.User;

public record UserSearchResultResponse(Long id, String username, String fullName, District district, Role role) {
    public static UserSearchResultResponse from(User user) {
        return new UserSearchResultResponse(user.getId(), user.getUsername(), user.getFullName(), user.getDistrict(), user.getRole());
    }
}
