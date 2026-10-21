package no.casevault.controller;

import jakarta.validation.Valid;
import no.casevault.config.JwtAuthenticationFilter;
import no.casevault.dto.ForgotPasswordRequest;
import no.casevault.dto.LoginRequest;
import no.casevault.dto.LoginResponse;
import no.casevault.dto.MessageResponse;
import no.casevault.dto.RegisterRequest;
import no.casevault.dto.ResetPasswordRequest;
import no.casevault.dto.UserResponse;
import no.casevault.model.User;
import no.casevault.service.AuthenticationService;
import no.casevault.service.JwtService;
import no.casevault.service.PasswordResetService;
import no.casevault.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;

    public AuthController(RegistrationService registrationService,
                           AuthenticationService authenticationService,
                           JwtService jwtService,
                           PasswordResetService passwordResetService) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authenticationService.authenticate(request);
        Instant issuedAt = Instant.now();
        Instant expiresAt = jwtService.expirationFor(issuedAt);
        String token = jwtService.issueToken(user, issuedAt);
        LoginResponse response = new LoginResponse(token, expiresAt, UserResponse.from(user));

        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, token)
            .httpOnly(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.between(issuedAt, expiresAt))
            .build();

        return ResponseEntity.ok().header("Set-Cookie", cookie.toString()).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, "")
            .httpOnly(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build();

        return ResponseEntity.ok().header("Set-Cookie", cookie.toString()).body(new MessageResponse("Logged out."));
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.username());
        return new MessageResponse("If that account exists, a reset code has been sent.");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return new MessageResponse("Password updated.");
    }
}
