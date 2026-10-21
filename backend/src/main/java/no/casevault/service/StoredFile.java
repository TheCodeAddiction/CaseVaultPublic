package no.casevault.service;

public record StoredFile(String storedFilename, String originalFilename, String contentType, long sizeBytes) {
}
