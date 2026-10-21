package no.casevault.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("No user found with id: " + id);
    }

    public UserNotFoundException(String username) {
        super("No user found with username: " + username);
    }
}
