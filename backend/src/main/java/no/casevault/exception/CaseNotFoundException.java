package no.casevault.exception;

import java.util.UUID;

public class CaseNotFoundException extends RuntimeException {
    public CaseNotFoundException(UUID id) {
        super("No case found with id: " + id);
    }

    public CaseNotFoundException(Long id) {
        super("No case found with id: " + id);
    }
}
