package no.casevault.dto;

import no.casevault.model.District;
import no.casevault.model.Role;
import no.casevault.model.User;

public record UserResponse(
    Long id,
    String username,
    String email,
    String fullName,
    District district,
    Role role,
    boolean hasProfilePicture,
    boolean active
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getDistrict(), user.getRole(),
            user.getProfilePictureFilename() != null,
            user.isActive()
        );
    }
}
