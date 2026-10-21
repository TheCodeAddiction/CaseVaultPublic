package no.casevault.exception;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(Long id) {
        super("No document found with id: " + id);
    }
}
