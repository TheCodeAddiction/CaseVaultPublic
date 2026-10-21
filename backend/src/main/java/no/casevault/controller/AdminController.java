package no.casevault.controller;

import jakarta.validation.Valid;
import no.casevault.dto.ActivityLogEntryResponse;
import no.casevault.dto.CaseSummaryResponse;
import no.casevault.dto.ChangeDistrictRequest;
import no.casevault.dto.ChangeRoleRequest;
import no.casevault.dto.CreateUserRequest;
import no.casevault.dto.DeleteOwnAccountRequest;
import no.casevault.dto.MessageResponse;
import no.casevault.dto.UpdateAccountRequest;
import no.casevault.dto.UserResponse;
import no.casevault.exception.InvalidCredentialsException;
import no.casevault.exception.InvalidFileException;
import no.casevault.exception.ProfilePictureNotFoundException;
import no.casevault.exception.UserNotFoundException;
import no.casevault.exception.UsernameTakenException;
import no.casevault.model.Role;
import no.casevault.model.User;
import no.casevault.repository.UserRepository;
import no.casevault.service.AuditLogService;
import no.casevault.service.CaseService;
import no.casevault.service.FileStorageService;
import no.casevault.service.PasswordHasher;
import no.casevault.service.StoredFile;
import no.casevault.service.UserDeletionService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String ENTITY_TYPE = "USER";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    private final UserDeletionService userDeletionService;
    private final CaseService caseService;

    public AdminController(UserRepository userRepository, PasswordHasher passwordHasher,
                            FileStorageService fileStorageService, AuditLogService auditLogService,
                            UserDeletionService userDeletionService, CaseService caseService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.fileStorageService = fileStorageService;
        this.auditLogService = auditLogService;
        this.userDeletionService = userDeletionService;
        this.caseService = caseService;
    }

    @PreAuthorize("hasAnyRole('OFFICER','SUPERVISOR','ADMIN')")
    @GetMapping("/users/me")
    public UserResponse viewOwnProfile(Authentication authentication) {
        return UserResponse.from(currentUser(authentication));
    }

    @PreAuthorize("hasAnyRole('OFFICER','SUPERVISOR','ADMIN')")
    @PutMapping(value = "/users/me", consumes = "multipart/form-data")
    public UserResponse updateOwnAccount(@Valid @ModelAttribute UpdateAccountRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        List<String> changes = new ArrayList<>();

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
            changes.add("full name");
        }
        if (request.email() != null) {
            user.setEmail(request.email());
            changes.add("email");
        }
        if (request.role() != null) {
            user.changeRole(request.role());
            changes.add("role");
        }
        if (request.newPassword() != null) {
            if (request.currentPassword() == null || !passwordHasher.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new InvalidCredentialsException();
            }
            user.changePassword(passwordHasher.hash(request.newPassword()));
            changes.add("password");
        }
        if (request.file() != null && !request.file().isEmpty()) {
            String safeContentType = safeImageContentType(request.file());
            StoredFile stored = storeFile(request.file());
            user.setProfilePicture(stored.storedFilename(), safeContentType);
            changes.add("profile picture");
        }

        User saved = userRepository.save(user);

        auditLogService.record(user, ENTITY_TYPE, user.getId().toString(), "ACCOUNT_UPDATED",
            "Updated: " + String.join(", ", changes));

        return UserResponse.from(saved);
    }

    @PreAuthorize("hasAnyRole('OFFICER','SUPERVISOR','ADMIN')")
    @GetMapping("/users/{id}/picture")
    public ResponseEntity<Resource> viewProfilePicture(@PathVariable Long id) {
        User target = findUserOrThrow(id);
        if (target.getProfilePictureFilename() == null) {
            throw new ProfilePictureNotFoundException();
        }

        Resource resource = fileStorageService.load(target.getProfilePictureFilename());
        MediaType contentType = target.getProfilePictureContentType() != null
            ? MediaType.parseMediaType(target.getProfilePictureContentType())
            : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok().contentType(contentType).body(resource);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @GetMapping("/users")
    public List<UserResponse> listUsers(Authentication authentication) {
        User caller = currentUser(authentication);
        if (caller.getRole() == Role.ADMIN) {
            return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
        }
        if (caller.getRole() == Role.SUPERVISOR) {
            return userRepository.findByDistrictAndRole(caller.getDistrict(), Role.OFFICER).stream()
                .map(UserResponse::from)
                .toList();
        }
        throw new AccessDeniedException("Only supervisors and admins can list users");
    }

    @GetMapping("/cases/overview")
    public List<CaseSummaryResponse> casesOverview() {
        return caseService.listAll().stream()
            .map(CaseSummaryResponse::from)
            .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{id}/activity")
    public List<ActivityLogEntryResponse> userActivity(@PathVariable Long id, Authentication authentication) {
        findUserOrThrow(id);

        return auditLogService.findFor(ENTITY_TYPE, id.toString()).stream()
            .map(ActivityLogEntryResponse::from)
            .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        User caller = currentUser(authentication);
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new UsernameTakenException(request.username());
        }

        User user = new User(
            request.username(),
            request.email(),
            request.fullName(),
            passwordHasher.hash(request.password()),
            request.district(),
            request.role()
        );
        User saved = userRepository.save(user);

        auditLogService.record(caller, ENTITY_TYPE, saved.getId().toString(), "USER_CREATED",
            "User created with role " + saved.getRole() + " in district " + saved.getDistrict());

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(saved));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/role")
    public UserResponse changeUserRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request,
                                        Authentication authentication) {
        User caller = currentUser(authentication);
        User target = findUserOrThrow(id);

        target.changeRole(request.role());
        User saved = userRepository.save(target);

        auditLogService.record(caller, ENTITY_TYPE, saved.getId().toString(), "ROLE_CHANGED", "Role changed to " + saved.getRole());

        return UserResponse.from(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/district")
    public UserResponse changeUserDistrict(@PathVariable Long id, @Valid @RequestBody ChangeDistrictRequest request,
                                            Authentication authentication) {
        User caller = currentUser(authentication);
        User target = findUserOrThrow(id);

        target.changeDistrict(request.district());
        User saved = userRepository.save(target);

        auditLogService.record(caller, ENTITY_TYPE, saved.getId().toString(), "DISTRICT_CHANGED", "District changed to " + saved.getDistrict());

        return UserResponse.from(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{id}/deactivate")
    public UserResponse deactivateUser(@PathVariable Long id, Authentication authentication) {
        User caller = currentUser(authentication);
        User target = findUserOrThrow(id);

        target.deactivate();
        User saved = userRepository.save(target);

        auditLogService.record(caller, ENTITY_TYPE, saved.getId().toString(), "USER_DEACTIVATED", "User deactivated");

        return UserResponse.from(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{id}/reactivate")
    public UserResponse reactivateUser(@PathVariable Long id, Authentication authentication) {
        User caller = currentUser(authentication);
        User target = findUserOrThrow(id);

        target.reactivate();
        User saved = userRepository.save(target);

        auditLogService.record(caller, ENTITY_TYPE, saved.getId().toString(), "USER_REACTIVATED", "User reactivated");

        return UserResponse.from(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public MessageResponse deleteUser(@PathVariable Long id, Authentication authentication) {
        User caller = currentUser(authentication);
        User target = findUserOrThrow(id);
        Long deletedId = target.getId();

        userDeletionService.deleteUser(target);
        auditLogService.record(caller, ENTITY_TYPE, deletedId.toString(), "USER_DELETED", "User deleted by admin");

        return new MessageResponse("User deleted.");
    }

    @PreAuthorize("hasAnyRole('OFFICER','SUPERVISOR','ADMIN')")
    @DeleteMapping("/users/me")
    public MessageResponse deleteOwnAccount(@Valid @RequestBody DeleteOwnAccountRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        if (!passwordHasher.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        userDeletionService.deleteUser(user);

        return new MessageResponse("Account deleted.");
    }

    private static final List<String> ALLOWED_PICTURE_CONTENT_TYPES = List.of("image/jpeg", "image/png");

    private static final Map<String, byte[]> PICTURE_MAGIC_BYTES = Map.of(
        "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
    );

    private String safeImageContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_PICTURE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidFileException("Profile pictures must be a .jpg, .jpeg, or .png image");
        }

        byte[] signature = PICTURE_MAGIC_BYTES.get(contentType.toLowerCase(Locale.ROOT));
        byte[] header = new byte[signature.length];
        try (var in = file.getInputStream()) {
            if (in.readNBytes(header, 0, header.length) < header.length) {
                throw new InvalidFileException("File contents do not match declared type: " + contentType);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                throw new InvalidFileException("File contents do not match declared type: " + contentType);
            }
        }

        return contentType;
    }

    private StoredFile storeFile(MultipartFile file) {
        try {
            return fileStorageService.store(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
            .orElseThrow(InvalidCredentialsException::new);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
