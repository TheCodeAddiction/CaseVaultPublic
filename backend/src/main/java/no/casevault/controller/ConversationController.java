package no.casevault.controller;

import no.casevault.dto.ConversationSummaryResponse;
import no.casevault.dto.DirectMessageResponse;
import no.casevault.exception.InvalidCredentialsException;
import no.casevault.model.Message;
import no.casevault.model.User;
import no.casevault.repository.UserRepository;
import no.casevault.service.ConversationService;
import no.casevault.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@PreAuthorize("hasAnyRole('OFFICER','SUPERVISOR','ADMIN')")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final UserRepository userRepository;

    public ConversationController(ConversationService conversationService, MessageService messageService,
                                   UserRepository userRepository) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<ConversationSummaryResponse> list(Authentication authentication) {
        return conversationService.listSummariesForUser(currentUser(authentication));
    }

    @GetMapping("/{id}/messages")
    public List<DirectMessageResponse> messages(@PathVariable UUID id, Authentication authentication) {
        User user = currentUser(authentication);
        return messageService.listMessages(user, id).stream().map(DirectMessageResponse::from).toList();
    }

    @PostMapping(value = "/{id}/messages", consumes = "multipart/form-data")
    public ResponseEntity<DirectMessageResponse> reply(
        @PathVariable UUID id,
        @RequestParam String body,
        @RequestParam(required = false) UUID relatedCaseId,
        @RequestParam(required = false) MultipartFile attachment,
        Authentication authentication
    ) {
        User sender = currentUser(authentication);
        Message message = messageService.sendToConversation(sender, id, body, relatedCaseId, attachment);
        return ResponseEntity.status(HttpStatus.CREATED).body(DirectMessageResponse.from(message));
    }

    @PostMapping("/{id}/read")
    public List<DirectMessageResponse> markRead(@PathVariable UUID id, Authentication authentication) {
        User user = currentUser(authentication);
        return messageService.markConversationRead(user, id).stream().map(DirectMessageResponse::from).toList();
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
            .orElseThrow(InvalidCredentialsException::new);
    }
}
