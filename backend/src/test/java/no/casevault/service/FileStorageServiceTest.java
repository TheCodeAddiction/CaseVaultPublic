package no.casevault.service;

import no.casevault.config.StorageProperties;
import no.casevault.exception.InvalidFileException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final StorageProperties properties = new StorageProperties(
        System.getProperty("java.io.tmpdir") + "/casevault-storage-test-" + System.nanoTime(),
        DataSize.ofMegabytes(10).toBytes()
    );
    private final FileStorageService service = new FileStorageService(properties);

    private static byte[] withJpegMagic(String rest) {
        byte[] restBytes = rest.getBytes();
        byte[] combined = new byte[JPEG_MAGIC.length + restBytes.length];
        System.arraycopy(JPEG_MAGIC, 0, combined, 0, JPEG_MAGIC.length);
        System.arraycopy(restBytes, 0, combined, JPEG_MAGIC.length, restBytes.length);
        return combined;
    }

    @Test
    void storesFileUnderAGeneratedNameNotTheOriginalFilename() throws IOException {
        var file = new MockMultipartFile("file", "evidence.jpg", "image/jpeg", withJpegMagic("fake image bytes"));

        var stored = service.store(file);

        assertThat(stored.storedFilename()).isNotEqualTo("evidence.jpg");
        assertThat(stored.originalFilename()).isEqualTo("evidence.jpg");
        assertThat(stored.contentType()).isEqualTo("image/jpeg");
        assertThat(stored.sizeBytes()).isEqualTo(file.getSize());
        assertThat(Files.exists(Path.of(properties.directory(), stored.storedFilename()))).isTrue();
    }

    @Test
    void rejectsDisallowedFileExtensions() {
        var file = new MockMultipartFile("file", "script.exe", "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> service.store(file)).isInstanceOf(InvalidFileException.class);
    }

    @Test
    void loadReturnsThePreviouslyStoredBytes() throws IOException {
        var file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello world".getBytes());
        var stored = service.store(file);

        byte[] loaded = service.load(stored.storedFilename()).getContentAsByteArray();

        assertThat(new String(loaded)).isEqualTo("hello world");
    }
}
