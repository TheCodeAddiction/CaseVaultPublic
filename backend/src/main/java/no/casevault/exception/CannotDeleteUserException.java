package no.casevault.exception;

public class CannotDeleteUserException extends RuntimeException {
    public CannotDeleteUserException() {
        super("Cannot delete a user with existing case, document, message, or activity history. Deactivate the account instead.");
    }
}
