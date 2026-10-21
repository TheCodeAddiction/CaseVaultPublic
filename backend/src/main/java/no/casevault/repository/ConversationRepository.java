package no.casevault.repository;

import no.casevault.model.Conversation;
import no.casevault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByUserAAndUserB(User userA, User userB);

    boolean existsByUserAOrUserB(User userA, User userB);

    @Query("select c from Conversation c where c.userA = :user or c.userB = :user")
    List<Conversation> findAllForUser(@Param("user") User user);
}
