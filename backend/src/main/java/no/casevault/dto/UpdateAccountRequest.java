package no.casevault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import no.casevault.model.Role;
import org.springframework.web.multipart.MultipartFile;

public record UpdateAccountRequest(
    @Size(min = 2, max = 100) String fullName,
    @Email String email,
    Role role,
    String currentPassword,
    @Size(min = 8, max = 100) String newPassword,
    MultipartFile file
) {
}
