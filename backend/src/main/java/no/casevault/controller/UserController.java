package no.casevault.controller;

import no.casevault.dto.UserResponse;
import no.casevault.dto.UserSearchResultResponse;
import no.casevault.exception.InvalidCredentialsException;
import no.casevault.model.User;
import no.casevault.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("hasAnyRole('OFFICER','SUPERVISOR','ADMIN')")
public class UserController {

    private static final int SEARCH_RESULT_LIMIT = 10;

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/me")
    public UserResponse me(Authentication authentication) {
        var user = userRepository.findByUsernameIgnoreCase(authentication.getName())
            .orElseThrow(InvalidCredentialsException::new);
        return UserResponse.from(user);
    }

    @GetMapping("/api/users/search")
    public List<UserSearchResultResponse> search(@RequestParam String q, Authentication authentication) {
        User caller = userRepository.findByUsernameIgnoreCase(authentication.getName())
            .orElseThrow(InvalidCredentialsException::new);

        if (q.isBlank()) {
            return List.of();
        }

        return userRepository.searchActiveByUsernameOrFullName(q).stream()
            .filter(u -> !u.getId().equals(caller.getId()))
            .limit(SEARCH_RESULT_LIMIT)
            .map(UserSearchResultResponse::from)
            .toList();
    }
}
