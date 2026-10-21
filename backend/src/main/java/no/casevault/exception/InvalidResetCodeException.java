package no.casevault.exception;

public class InvalidResetCodeException extends RuntimeException {
    public InvalidResetCodeException() {
        super("Invalid or expired reset code");
    }
}
