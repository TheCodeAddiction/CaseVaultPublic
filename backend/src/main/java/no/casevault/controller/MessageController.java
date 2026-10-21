package no.casevault.controller;

import no.casevault.dto.DirectMessageResponse;
import no.casevault.dto.UnreadCountResponse;
import no.casevault.exception.InvalidCredentialsException;
import no.casevault.exception.MessageNotFoundException;
import no.casevault.model.Message;
import no.casevault.model.User;
import no.casevault.repository.UserRepository;
import no.casevault.service.FileStorageService;
import no.casevault.service.MessageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@PreAuthorize("hasAnyRole('OFFICER','SUPERVISOR','ADMIN')")
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public MessageController(MessageService messageService, UserRepository userRepository,
                              FileStorageService fileStorageService) {
        this.messageService = messageService;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DirectMessageResponse> sendMessage(
        @RequestParam String recipientUsername,
        @RequestParam String body,
        @RequestParam(required = false) UUID relatedCaseId,
        @RequestParam(required = false) MultipartFile attachment,
        Authentication authentication
    ) {
        User sender = currentUser(authentication);
        Message message = messageService.sendNewMessage(sender, recipientUsername, body, relatedCaseId, attachment);
        return ResponseEntity.status(HttpStatus.CREATED).body(DirectMessageResponse.from(message));
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(Authentication authentication) {
        User user = currentUser(authentication);
        return new UnreadCountResponse(messageService.unreadCount(user));
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID id, Authentication authentication) {
        User user = currentUser(authentication);
        Message message = messageService.findForParticipant(user, id);

        if (message.getAttachmentStoredFilename() == null) {
            throw new MessageNotFoundException(id);
        }

        Resource resource = fileStorageService.load(message.getAttachmentStoredFilename());

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(message.getAttachmentContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + message.getAttachmentOriginalFilename() + "\"")
            .body(resource);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
            .orElseThrow(InvalidCredentialsException::new);
    }
}
