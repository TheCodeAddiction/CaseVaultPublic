package no.casevault.controller;

import no.casevault.model.PasswordResetCode;
import no.casevault.model.User;
import no.casevault.repository.PasswordResetCodeRepository;
import no.casevault.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetCodeRepository passwordResetCodeRepository;

    @Test
    void registerCreatesOfficerAccountAndReturnsItWithoutPassword() throws Exception {
        var request = """
            {
              "fullName": "Maria Ibsen",
              "username": "mibsen",
              "email": "mibsen@example.test",
              "password": "correct horse battery staple",
              "confirmPassword": "correct horse battery staple",
              "district": "NORTH"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("mibsen"))
            .andExpect(jsonPath("$.email").value("mibsen@example.test"))
            .andExpect(jsonPath("$.fullName").value("Maria Ibsen"))
            .andExpect(jsonPath("$.district").value("NORTH"))
            .andExpect(jsonPath("$.role").value("OFFICER"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());

        Optional<User> saved = userRepository.findByUsernameIgnoreCase("mibsen");
        assertThat(saved).isPresent();
        assertThat(saved.get().getPasswordHash()).isNotEqualTo("correct horse battery staple");
    }

    @Test
    void registerRejectsMismatchedPasswordConfirmation() throws Exception {
        var request = """
            {
              "fullName": "Jon Berg",
              "username": "jberg",
              "email": "jberg@example.test",
              "password": "correct horse battery staple",
              "confirmPassword": "does not match",
              "district": "EAST"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("PASSWORD_MISMATCH"));

        assertThat(userRepository.findByUsernameIgnoreCase("jberg")).isEmpty();
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        var request = """
            {
              "fullName": "Ola Nord",
              "username": "onord",
              "email": "onord@example.test",
              "password": "correct horse battery staple",
              "confirmPassword": "correct horse battery staple",
              "district": "WEST"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(request))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(request))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("USERNAME_TAKEN"));
    }

    @Test
    void registerRejectsInvalidFields() throws Exception {
        var request = """
            {
              "fullName": "A",
              "username": "x",
              "email": "not-an-email",
              "password": "short",
              "confirmPassword": "short",
              "district": "NORTH"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fields.fullName").exists())
            .andExpect(jsonPath("$.fields.email").exists())
            .andExpect(jsonPath("$.fields.username").exists())
            .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void loginWithValidCredentialsReturnsTokenAndUser() throws Exception {
        registerUser("kolsen", "correct horse battery staple", "Kari Olsen", "SOUTH");

        var request = """
            {
              "username": "kolsen",
              "password": "correct horse battery staple"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.expiresAt").isNotEmpty())
            .andExpect(jsonPath("$.user.username").value("kolsen"))
            .andExpect(jsonPath("$.user.fullName").value("Kari Olsen"))
            .andExpect(jsonPath("$.user.district").value("SOUTH"))
            .andExpect(jsonPath("$.user.role").value("OFFICER"));
    }

    @Test
    void loginWithWrongPasswordReturnsGenericInvalidCredentialsError() throws Exception {
        registerUser("perhansen", "correct horse battery staple", "Per Hansen", "NORTH");

        var request = """
            {
              "username": "perhansen",
              "password": "wrong password"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(request))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void loginWithUnknownUsernameReturnsSameGenericInvalidCredentialsError() throws Exception {
        var request = """
            {
              "username": "does-not-exist",
              "password": "whatever it is"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(request))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void forgotPasswordThenResetPasswordWithCorrectCodeChangesPassword() throws Exception {
        registerUser("resetuser1", "correct horse battery staple", "Reset User", "NORTH");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType("application/json")
                .content("{\"username\": \"resetuser1\"}"))
            .andExpect(status().isOk());

        User user = userRepository.findByUsernameIgnoreCase("resetuser1").orElseThrow();
        PasswordResetCode resetCode = passwordResetCodeRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow();

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType("application/json")
                .content("{\"username\": \"resetuser1\", \"code\": \"" + resetCode.getCode() + "\", \"newPassword\": \"a brand new password\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"resetuser1\", \"password\": \"a brand new password\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void forgotPasswordForUnknownUsernameStillReturnsOk() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType("application/json")
                .content("{\"username\": \"does-not-exist\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void resetPasswordRejectsWrongCode() throws Exception {
        registerUser("resetuser2", "correct horse battery staple", "Reset User 2", "NORTH");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType("application/json")
                .content("{\"username\": \"resetuser2\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType("application/json")
                .content("{\"username\": \"resetuser2\", \"code\": \"0000\", \"newPassword\": \"a brand new password\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_RESET_CODE"));

        User user = userRepository.findByUsernameIgnoreCase("resetuser2").orElseThrow();
        PasswordResetCode resetCode = passwordResetCodeRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow();
        assertThat(resetCode.getCode()).isNotEqualTo("0000");
    }

    @Test
    void resetPasswordRejectsAfterTenFailedAttemptsEvenWithCorrectCode() throws Exception {
        registerUser("resetuser3", "correct horse battery staple", "Reset User 3", "NORTH");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType("application/json")
                .content("{\"username\": \"resetuser3\"}"))
            .andExpect(status().isOk());

        User user = userRepository.findByUsernameIgnoreCase("resetuser3").orElseThrow();
        PasswordResetCode resetCode = passwordResetCodeRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow();
        String wrongCode = resetCode.getCode().equals("1111") ? "2222" : "1111";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/reset-password")
                    .contentType("application/json")
                    .content("{\"username\": \"resetuser3\", \"code\": \"" + wrongCode + "\", \"newPassword\": \"a brand new password\"}"))
                .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType("application/json")
                .content("{\"username\": \"resetuser3\", \"code\": \"" + resetCode.getCode() + "\", \"newPassword\": \"a brand new password\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_RESET_CODE"));
    }

    @Test
    void resetPasswordRejectsExpiredCode() throws Exception {
        registerUser("resetuser4", "correct horse battery staple", "Reset User 4", "NORTH");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType("application/json")
                .content("{\"username\": \"resetuser4\"}"))
            .andExpect(status().isOk());

        User user = userRepository.findByUsernameIgnoreCase("resetuser4").orElseThrow();
        PasswordResetCode resetCode = passwordResetCodeRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow();

        passwordResetCodeRepository.delete(resetCode);
        passwordResetCodeRepository.save(new PasswordResetCode(user, resetCode.getCode(),
            Instant.now().minus(20, ChronoUnit.MINUTES), Instant.now().minus(10, ChronoUnit.MINUTES)));

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType("application/json")
                .content("{\"username\": \"resetuser4\", \"code\": \"" + resetCode.getCode() + "\", \"newPassword\": \"a brand new password\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_RESET_CODE"));
    }

    @Test
    void locksAccountAfterFiveFailedAttemptsWithSameGenericError() throws Exception {
        registerUser("lockeduser1", "correct horse battery staple", "Locked User", "NORTH");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType("application/json")
                    .content("{\"username\": \"lockeduser1\", \"password\": \"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
        }

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"lockeduser1\", \"password\": \"correct horse battery staple\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));

        User user = userRepository.findByUsernameIgnoreCase("lockeduser1").orElseThrow();
        assertThat(user.isLockedAt(Instant.now())).isTrue();
    }

    @Test
    void lockExpiresAfterItsDuration() throws Exception {
        registerUser("lockeduser2", "correct horse battery staple", "Locked User 2", "NORTH");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType("application/json")
                    .content("{\"username\": \"lockeduser2\", \"password\": \"wrong\"}"))
                .andExpect(status().isUnauthorized());
        }

        User user = userRepository.findByUsernameIgnoreCase("lockeduser2").orElseThrow();
        assertThat(user.isLockedAt(Instant.now())).isTrue();
        // Simulate the lockout window having already passed.
        assertThat(user.isLockedAt(Instant.now().plus(20, ChronoUnit.MINUTES))).isFalse();
    }

    @Test
    void successfulLoginResetsFailedAttemptCounter() throws Exception {
        registerUser("resetcounteruser1", "correct horse battery staple", "Reset Counter User", "NORTH");

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"resetcounteruser1\", \"password\": \"wrong\"}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"resetcounteruser1\", \"password\": \"wrong\"}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"resetcounteruser1\", \"password\": \"correct horse battery staple\"}"))
            .andExpect(status().isOk());

        // Three more wrong attempts shouldn't lock the account, since the
        // successful login above should have reset the counter to zero.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType("application/json")
                    .content("{\"username\": \"resetcounteruser1\", \"password\": \"wrong\"}"))
                .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"resetcounteruser1\", \"password\": \"correct horse battery staple\"}"))
            .andExpect(status().isOk());
    }

    private void registerUser(String username, String password, String fullName, String district) throws Exception {
        var request = String.format("""
            {
              "fullName": "%s",
              "username": "%s",
              "email": "%s@example.test",
              "password": "%s",
              "confirmPassword": "%s",
              "district": "%s"
            }
            """, fullName, username, username, password, password, district);

        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(request))
            .andExpect(status().isCreated());
    }
}
