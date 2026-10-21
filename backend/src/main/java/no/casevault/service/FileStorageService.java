package no.casevault.service;

import no.casevault.config.StorageProperties;
import no.casevault.exception.InvalidFileException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
        "image/jpeg", "image/png", "application/pdf", "text/plain"
    );

    // Leading-byte signatures used to confirm the declared Content-Type matches the
    // file's actual contents. No fixed signature exists for text/plain, so it's exempt.
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
        "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
        "application/pdf", new byte[] {0x25, 0x50, 0x44, 0x46}
    );

    private final StorageProperties properties;

    public FileStorageService(StorageProperties properties) {
        this.properties = properties;
    }

    public StoredFile store(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
        String extension = extensionOf(originalFilename);

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidFileException("Unsupported file type: " + contentType);
        }
        if (!matchesMagicBytes(file, contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidFileException("File contents do not match declared type: " + contentType);
        }
        if (file.getSize() > properties.maxFileSizeBytes()) {
            throw new InvalidFileException("File exceeds the maximum allowed size");
        }

        Path directory = Path.of(properties.directory());
        Files.createDirectories(directory);

        String storedFilename = UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT);
        Path target = directory.resolve(storedFilename);
        file.transferTo(target);

        return new StoredFile(storedFilename, originalFilename, file.getContentType(), file.getSize());
    }

    public Resource load(String storedFilename) {
        Path target = Path.of(properties.directory()).resolve(storedFilename).normalize();
        if (!target.startsWith(Path.of(properties.directory()).normalize())) {
            throw new InvalidFileException("Invalid stored filename");
        }
        return new FileSystemResource(target);
    }

    private boolean matchesMagicBytes(MultipartFile file, String contentType) throws IOException {
        byte[] signature = MAGIC_BYTES.get(contentType);
        if (signature == null) {
            return true;
        }
        byte[] header = new byte[signature.length];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(header, 0, header.length);
            if (read < header.length) {
                return false;
            }
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private String extensionOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new InvalidFileException("File must have an extension");
        }
        return filename.substring(dotIndex + 1);
    }
}
