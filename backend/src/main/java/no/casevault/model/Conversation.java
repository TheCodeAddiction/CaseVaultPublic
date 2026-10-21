package no.casevault.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations", uniqueConstraints = @UniqueConstraint(columnNames = {"user_a_id", "user_b_id"}))
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_a_id", nullable = false)
    private User userA;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_b_id", nullable = false)
    private User userB;

    @Column(nullable = false)
    private Instant createdAt;

    protected Conversation() {
    }

    public Conversation(User userA, User userB, Instant createdAt) {
        this.userA = userA;
        this.userB = userB;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getUserA() {
        return userA;
    }

    public User getUserB() {
        return userB;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean hasParticipant(User user) {
        return userA.getId().equals(user.getId()) || userB.getId().equals(user.getId());
    }

    public User otherParticipant(User user) {
        return userA.getId().equals(user.getId()) ? userB : userA;
    }
}
