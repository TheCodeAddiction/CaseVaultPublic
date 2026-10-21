package no.casevault.repository;

import no.casevault.model.Conversation;
import no.casevault.model.Message;
import no.casevault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    long countByRecipientAndReadAtIsNull(User recipient);

    boolean existsBySenderOrRecipient(User sender, User recipient);

    List<Message> findByConversationOrderBySentAtAsc(Conversation conversation);

    Optional<Message> findTopByConversationOrderBySentAtDesc(Conversation conversation);

    long countByConversationAndRecipientAndReadAtIsNull(Conversation conversation, User recipient);

    List<Message> findByConversationAndRecipientAndReadAtIsNull(Conversation conversation, User recipient);
}
