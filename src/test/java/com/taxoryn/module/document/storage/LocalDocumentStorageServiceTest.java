package com.taxoryn.module.document.storage;

import com.taxoryn.core.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalDocumentStorageService storageService;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.getLocal().setBaseDir(tempDir.toString());

        storageService = new LocalDocumentStorageService(properties);
        storageService.init();
    }

    @Test
    @DisplayName("Store, retrieve, and delete file locally")
    void testStoreRetrieveAndDeleteFile() {
        UUID organizationId = UUID.randomUUID();
        byte[] content = "Form 16 Tax Statement Sample Content".getBytes(StandardCharsets.UTF_8);

        String storageKey = storageService.store(organizationId, "form16.pdf", "application/pdf", content);
        assertNotNull(storageKey);
        assertTrue(storageKey.contains("org_" + organizationId));
        assertTrue(storageService.exists(storageKey));

        byte[] retrieved = storageService.retrieve(storageKey);
        assertArrayEquals(content, retrieved);
        assertEquals("LOCAL", storageService.getStorageProviderName());

        storageService.delete(storageKey);
        assertFalse(storageService.exists(storageKey));
    }

    @Test
    @DisplayName("Empty file content throws BadRequestException")
    void testEmptyFileThrowsBadRequest() {
        UUID organizationId = UUID.randomUUID();
        assertThrows(BadRequestException.class, () ->
                storageService.store(organizationId, "empty.pdf", "application/pdf", new byte[0]));
    }
}
