package no.casevault.exception;

import java.util.UUID;

public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException(UUID id) {
        super("No conversation found with id: " + id);
    }
}
