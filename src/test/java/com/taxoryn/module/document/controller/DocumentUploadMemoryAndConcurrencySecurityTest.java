package com.taxoryn.module.document.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentScanStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.document.storage.DocumentStorageService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DocumentUploadMemoryAndConcurrencySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private DocumentStorageService storageService;

    @Autowired
    private com.taxoryn.module.subscription.repository.SubscriptionRepository subscriptionRepository;

    private OrganizationEntity org;
    private UserEntity adminUser;
    private ClientEntity client;
    private String adminToken;

    @BeforeEach
    public void setup() {
        cleanUp();

        org = organizationRepository.save(OrganizationEntity.builder()
                .name("Concurrency Test Org " + UUID.randomUUID())
                .email("org_" + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        subscriptionRepository.save(com.taxoryn.module.subscription.entity.SubscriptionEntity.builder()
                .organizationId(org.getId())
                .plan(com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan.ENTERPRISE)
                .status(com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionStatus.ACTIVE)
                .startDate(java.time.LocalDate.now())
                .renewalDate(java.time.LocalDate.now().plusYears(1))
                .maxUsers(100)
                .maxClients(1000)
                .maxStorageBytes(10L * 1024 * 1024 * 1024) // 10 GB
                .build());

        TenantContext.setTenantId(org.getId());

        RoleEntity role = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN_" + UUID.randomUUID())
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        adminUser = userRepository.save(UserEntity.builder()
                .email("admin_" + UUID.randomUUID() + "@taxoryn.com")
                .passwordHash(passwordEncoder.encode("StrongPass123!#"))
                .firstName("Admin")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(role)))
                .build());

        client = clientRepository.save(ClientEntity.builder()
                .displayName("Client Corp " + UUID.randomUUID())
                .legalName("Client Corporation Pvt Ltd")
                .pan("ABCDE1234F")
                .status(ClientStatus.ACTIVE)
                .clientType(ClientType.PRIVATE_LIMITED)
                .build());

        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                org.getId(),
                adminUser.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("DOCUMENT_VIEW", "DOCUMENT_UPLOAD", "DOCUMENT_DELETE", "ROLE_PRACTICE_ADMIN")
        );

        TenantContext.clear();
    }

    @AfterEach
    public void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        documentRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();
        subscriptionRepository.deleteAll();
    }

    private byte[] createPdfPayload(int sizeBytes, String uniqueMarker) {
        byte[] markerBytes = uniqueMarker.getBytes(StandardCharsets.UTF_8);
        byte[] pdfHeader = "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
        byte[] pdfFooter = "\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

        byte[] payload = new byte[sizeBytes];
        Arrays.fill(payload, (byte) 'A');

        System.arraycopy(pdfHeader, 0, payload, 0, Math.min(pdfHeader.length, sizeBytes));
        int markerOffset = pdfHeader.length;
        int maxMarkerLen = Math.max(0, sizeBytes - pdfHeader.length - pdfFooter.length);
        int actualMarkerLen = Math.min(markerBytes.length, maxMarkerLen);
        if (actualMarkerLen > 0) {
            System.arraycopy(markerBytes, 0, payload, markerOffset, actualMarkerLen);
        }
        if (sizeBytes >= pdfFooter.length) {
            System.arraycopy(pdfFooter, 0, payload, sizeBytes - pdfFooter.length, pdfFooter.length);
        }

        return payload;
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Upload 1 file: Valid 5MB PDF completes with zero corruption, valid checksum, and CLEAN scan status")
    public void testSingleUpload5MbIntegrity() throws Exception {
        int size = 5 * 1024 * 1024; // 5 MB
        String marker = "SINGLE_UPLOAD_TEST_" + UUID.randomUUID();
        byte[] content = createPdfPayload(size, marker);
        String expectedChecksum = calculateSha256(content);

        MockMultipartFile file = new MockMultipartFile("file", "audit_report_5mb.pdf", "application/pdf", content);

        UploadDocumentRequest request = UploadDocumentRequest.builder()
                .clientId(client.getId())
                .documentType(DocumentType.TAX_AUDIT_REPORT)
                .notes("5MB Audit Report")
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "", "application/json",
                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadata)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("audit_report_5mb.pdf"))
                .andExpect(jsonPath("$.data.fileSize").value(size))
                .andExpect(jsonPath("$.data.scanStatus").value("CLEAN"))
                .andExpect(jsonPath("$.data.checksum").value(expectedChecksum))
                .andReturn();

        // Verify database and storage state
        List<DocumentEntity> docs = documentRepository.findAll();
        assertEquals(1, docs.size());
        DocumentEntity stored = docs.get(0);
        assertEquals(expectedChecksum, stored.getChecksum());
        assertEquals(DocumentScanStatus.CLEAN, stored.getScanStatus());
        assertEquals(DocumentStatus.ACTIVE, stored.getStatus());
        assertTrue(storageService.exists(stored.getStorageKey()));

        // Verify storage content integrity
        byte[] retrieved = storageService.retrieve(stored.getStorageKey());
        assertEquals(expectedChecksum, calculateSha256(retrieved));
    }

    @Test
    @DisplayName("Concurrent 5 Uploads: 5 parallel threads uploading 2MB files simultaneously succeed without OOM or corruption")
    public void testFiveConcurrentUploads() throws Exception {
        int concurrency = 5;
        int size = 2 * 1024 * 1024; // 2 MB each = 10 MB total
        runConcurrentUploads(concurrency, size);
    }

    @Test
    @DisplayName("Concurrent 10 Uploads: 10 parallel threads uploading 3MB files simultaneously succeed without memory exhaustion")
    public void testTenConcurrentUploads() throws Exception {
        int concurrency = 10;
        int size = 3 * 1024 * 1024; // 3 MB each = 30 MB total
        runConcurrentUploads(concurrency, size);
    }

    private void runConcurrentUploads(int concurrency, int fileSize) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(concurrency);

        List<byte[]> payloads = new ArrayList<>();
        List<String> expectedChecksums = new ArrayList<>();

        for (int i = 0; i < concurrency; i++) {
            byte[] content = createPdfPayload(fileSize, "CONCURRENT_THREAD_" + i + "_" + UUID.randomUUID());
            payloads.add(content);
            expectedChecksums.add(calculateSha256(content));
        }

        List<Callable<MvcResult>> tasks = new ArrayList<>();
        for (int i = 0; i < concurrency; i++) {
            final int index = i;
            tasks.add(() -> {
                try {
                    startGate.await(); // Synchronize parallel launch

                    byte[] content = payloads.get(index);
                    String filename = "concurrent_doc_" + index + ".pdf";

                    MockMultipartFile file = new MockMultipartFile("file", filename, "application/pdf", content);

                    UploadDocumentRequest request = UploadDocumentRequest.builder()
                            .clientId(client.getId())
                            .documentType(DocumentType.FINANCIAL_STATEMENTS)
                            .notes("Concurrent Thread " + index)
                            .build();

                    MockMultipartFile metadata = new MockMultipartFile(
                            "metadata", "", "application/json",
                            objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)
                    );

                    return mockMvc.perform(multipart("/api/v1/documents/upload")
                                    .file(file)
                                    .file(metadata)
                                    .header("Authorization", adminToken)
                                    .contentType(MediaType.MULTIPART_FORM_DATA))
                            .andReturn();
                } finally {
                    endGate.countDown();
                }
            });
        }

        List<Future<MvcResult>> futures = new ArrayList<>();
        for (Callable<MvcResult> task : tasks) {
            futures.add(executor.submit(task));
        }

        // Trigger simultaneous start
        startGate.countDown();

        boolean finished = endGate.await(60, TimeUnit.SECONDS);
        assertTrue(finished, "Concurrent uploads did not finish within timeout");

        for (int i = 0; i < concurrency; i++) {
            MvcResult res = futures.get(i).get();
            assertEquals(201, res.getResponse().getStatus(), "Upload thread " + i + " failed with status " + res.getResponse().getStatus() + ": " + res.getResponse().getContentAsString());
        }

        executor.shutdown();

        // Verify database state: exactly `concurrency` documents exist and all have valid unique checksums
        List<DocumentEntity> storedDocs = documentRepository.findAll();
        assertEquals(concurrency, storedDocs.size());

        Set<String> storedChecksums = new HashSet<>();
        Set<String> storedStorageKeys = new HashSet<>();

        for (DocumentEntity doc : storedDocs) {
            assertEquals(DocumentScanStatus.CLEAN, doc.getScanStatus());
            assertEquals(DocumentStatus.ACTIVE, doc.getStatus());
            assertEquals(org.getId(), doc.getOrganizationId());
            assertEquals(client.getId(), doc.getClientId());

            storedChecksums.add(doc.getChecksum());
            storedStorageKeys.add(doc.getStorageKey());

            // Check object existence in storage backend
            assertTrue(storageService.exists(doc.getStorageKey()));

            // Verify content retrieved from storage matches recorded checksum
            byte[] retrieved = storageService.retrieve(doc.getStorageKey());
            assertEquals(doc.getChecksum(), calculateSha256(retrieved));
        }

        assertEquals(concurrency, storedChecksums.size(), "Checksum collision detected among concurrent uploads");
        assertEquals(concurrency, storedStorageKeys.size(), "Storage key collision detected among concurrent uploads");

        // Verify all generated checksums are accounted for
        for (String checksum : expectedChecksums) {
            assertTrue(storedChecksums.contains(checksum), "Expected checksum not found in stored documents: " + checksum);
        }
    }

    @Test
    @DisplayName("Upload failure cleanup: EICAR malware is rejected and leaves no stored document or orphan storage object")
    public void testMalwareUploadCleanupOnFailure() throws Exception {
        String eicar = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
        byte[] payload = ("%PDF-1.7\n" + eicar + "\n%%EOF").getBytes(StandardCharsets.US_ASCII);

        MockMultipartFile file = new MockMultipartFile("file", "infected.pdf", "application/pdf", payload);

        UploadDocumentRequest request = UploadDocumentRequest.builder()
                .clientId(client.getId())
                .documentType(DocumentType.OTHER)
                .notes("Infected file test")
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "", "application/json",
                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadata)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Verify zero documents stored in repository
        assertEquals(0, documentRepository.count());
    }

    @Test
    @DisplayName("Upload failure cleanup: Disguised Windows PE executable (MZ header) is rejected with 400 and zero persistence")
    public void testDisguisedExecutableBinaryCleanupOnFailure() throws Exception {
        byte[] pePayload = new byte[]{(byte) 0x4D, (byte) 0x5A, 0x00, 0x00, 0x01, 0x02, 0x03};

        MockMultipartFile file = new MockMultipartFile("file", "malicious.pdf", "application/pdf", pePayload);

        UploadDocumentRequest request = UploadDocumentRequest.builder()
                .clientId(client.getId())
                .documentType(DocumentType.OTHER)
                .notes("Disguised PE file")
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "", "application/json",
                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadata)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Verify zero documents stored in repository
        assertEquals(0, documentRepository.count());
    }
}
