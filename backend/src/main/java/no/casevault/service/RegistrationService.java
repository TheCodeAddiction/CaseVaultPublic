package no.casevault.service;

import no.casevault.dto.RegisterRequest;
import no.casevault.exception.PasswordMismatchException;
import no.casevault.exception.UsernameTakenException;
import no.casevault.model.Role;
import no.casevault.model.User;
import no.casevault.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegistrationService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new PasswordMismatchException();
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new UsernameTakenException(request.username());
        }

        User user = new User(
            request.username(),
            request.email(),
            request.fullName(),
            passwordHasher.hash(request.password()),
            request.district(),
            Role.OFFICER
        );
        return userRepository.save(user);
    }
}
