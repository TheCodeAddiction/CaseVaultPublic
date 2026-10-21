package no.casevault.service;

import no.casevault.dto.ResetPasswordRequest;
import no.casevault.exception.InvalidResetCodeException;
import no.casevault.model.PasswordResetCode;
import no.casevault.model.User;
import no.casevault.repository.PasswordResetCodeRepository;
import no.casevault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int CODE_LIFETIME_MINUTES = 10;

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository resetCodeRepository;
    private final PasswordHasher passwordHasher;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository userRepository, PasswordResetCodeRepository resetCodeRepository,
                                 PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.resetCodeRepository = resetCodeRepository;
        this.passwordHasher = passwordHasher;
    }

    public void requestReset(String username) {
        userRepository.findByUsernameIgnoreCase(username).ifPresent(user -> {
            Instant now = Instant.now();
            String code = generateCode();
            resetCodeRepository.save(new PasswordResetCode(user, code, now, now.plus(CODE_LIFETIME_MINUTES, ChronoUnit.MINUTES)));

            log.info("Password reset code for user '{}': {}", user.getUsername(), code);
        });
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.username())
            .orElseThrow(InvalidResetCodeException::new);

        PasswordResetCode resetCode = resetCodeRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
            .orElseThrow(InvalidResetCodeException::new);

        if (resetCode.isExpired(Instant.now()) || !resetCode.hasAttemptsRemaining()) {
            throw new InvalidResetCodeException();
        }

        if (!resetCode.getCode().equals(request.code())) {
            resetCode.registerFailedAttempt();
            resetCodeRepository.save(resetCode);
            throw new InvalidResetCodeException();
        }

        resetCode.markUsed();
        resetCodeRepository.save(resetCode);

        user.changePassword(passwordHasher.hash(request.newPassword()));
        userRepository.save(user);
    }

    private String generateCode() {
        return String.format("%04d", random.nextInt(10_000));
    }
}
