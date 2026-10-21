package no.casevault.service;

import no.casevault.dto.LoginRequest;
import no.casevault.exception.InvalidCredentialsException;
import no.casevault.model.User;
import no.casevault.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthenticationService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User authenticate(LoginRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.username())
            .orElseThrow(InvalidCredentialsException::new);

        Instant now = Instant.now();

        if (user.isLockedAt(now)) {
            throw new InvalidCredentialsException();
        }

        if (!passwordHasher.matches(request.password(), user.getPasswordHash())) {
            user.registerFailedLogin(now);
            userRepository.save(user);
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        user.registerSuccessfulLogin();
        userRepository.save(user);

        return user;
    }
}
