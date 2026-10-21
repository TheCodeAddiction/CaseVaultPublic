package no.casevault.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "password_reset_codes")
public class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private boolean used = false;

    protected PasswordResetCode() {
    }

    public PasswordResetCode(User user, String code, Instant createdAt, Instant expiresAt) {
        this.user = user;
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getCode() {
        return code;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public boolean isUsed() {
        return used;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public void registerFailedAttempt() {
        this.attempts++;
    }

    public boolean hasAttemptsRemaining() {
        return attempts < 10;
    }

    public void markUsed() {
        this.used = true;
    }
}
