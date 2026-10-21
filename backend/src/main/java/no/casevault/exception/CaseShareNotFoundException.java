package no.casevault.exception;

public class CaseShareNotFoundException extends RuntimeException {
    public CaseShareNotFoundException(Long id) {
        super("No case share found with id: " + id);
    }
}
