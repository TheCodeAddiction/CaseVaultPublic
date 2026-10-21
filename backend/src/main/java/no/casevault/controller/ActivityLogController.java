package no.casevault.controller;

import jakarta.validation.Valid;
import no.casevault.dto.MessageResponse;
import no.casevault.dto.RecordActivityRequest;
import no.casevault.exception.InvalidCredentialsException;
import no.casevault.model.User;
import no.casevault.repository.UserRepository;
import no.casevault.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity-log")
@PreAuthorize("hasAnyRole('OFFICER','SUPERVISOR','ADMIN')")
public class ActivityLogController {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public ActivityLogController(AuditLogService auditLogService, UserRepository userRepository) {
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> record(@Valid @RequestBody RecordActivityRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        auditLogService.record(user, request.entityType(), request.entityId(), request.action(), request.details());

        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Activity recorded."));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
            .orElseThrow(InvalidCredentialsException::new);
    }
}
