package no.casevault.controller;

import no.casevault.model.*;
import no.casevault.repository.CaseRepository;
import no.casevault.repository.UserRepository;
import no.casevault.service.PasswordHasher;
import no.casevault.testsupport.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocumentControllerTest {

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private static byte[] withJpegMagic(String rest) {
        byte[] restBytes = rest.getBytes();
        byte[] combined = new byte[JPEG_MAGIC.length + restBytes.length];
        System.arraycopy(JPEG_MAGIC, 0, combined, 0, JPEG_MAGIC.length);
        System.arraycopy(restBytes, 0, combined, JPEG_MAGIC.length, restBytes.length);
        return combined;
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    void addMessageDocumentAppearsInListAndLogsActivity() throws Exception {
        UUID caseId = seedCase("NORTH-2026-0100", District.NORTH);
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "docofficer1", District.NORTH);

        mockMvc.perform(multipart("/api/cases/" + caseId + "/documents")
                .param("type", "MESSAGE")
                .param("title", "Update")
                .param("content", "Spoke with the witness again, no new leads.")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("MESSAGE"))
            .andExpect(jsonPath("$.content").value("Spoke with the witness again, no new leads."))
            .andExpect(jsonPath("$.authorUsername").value("docofficer1"));

        mockMvc.perform(get("/api/cases/" + caseId + "/documents").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/cases/" + caseId + "/activity").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].action").value("COMMENT_ADDED"))
            .andExpect(jsonPath("$[0].details").value("Added a comment: Update"));
    }

    @Test
    void addMessageDocumentRequiresContent() throws Exception {
        UUID caseId = seedCase("NORTH-2026-0101", District.NORTH);
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "docofficer2", District.NORTH);

        mockMvc.perform(multipart("/api/cases/" + caseId + "/documents")
                .param("type", "MESSAGE")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_DOCUMENT"));
    }

    @Test
    void addFileDocumentStoresAndAllowsDownload() throws Exception {
        UUID caseId = seedCase("NORTH-2026-0102", District.NORTH);
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "docofficer3", District.NORTH);

        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", withJpegMagic("fake jpeg bytes"));

        String responseBody = mockMvc.perform(multipart("/api/cases/" + caseId + "/documents")
                .file(file)
                .param("type", "FILE")
                .param("title", "Scene photo")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("FILE"))
            .andExpect(jsonPath("$.originalFilename").value("photo.jpg"))
            .andReturn().getResponse().getContentAsString();

        Number docIdNumber = com.jayway.jsonpath.JsonPath.read(responseBody, "$.id");
        Long documentId = docIdNumber.longValue();

        mockMvc.perform(get("/api/cases/" + caseId + "/documents/" + documentId + "/file")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"photo.jpg\""));
    }

    @Test
    void addFileDocumentRejectsDisallowedExtension() throws Exception {
        UUID caseId = seedCase("NORTH-2026-0103", District.NORTH);
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "docofficer4", District.NORTH);

        var file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "x".getBytes());

        mockMvc.perform(multipart("/api/cases/" + caseId + "/documents")
                .file(file)
                .param("type", "FILE")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_FILE"));
    }

    @Test
    void authorCanDeleteOwnComment() throws Exception {
        UUID caseId = seedCase("NORTH-2026-0105", District.NORTH);
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "docofficer6", District.NORTH);
        Long documentId = addComment(caseId, token);

        mockMvc.perform(delete("/api/cases/" + caseId + "/documents/" + documentId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/cases/" + caseId + "/documents").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void otherOfficerCannotDeleteSomeoneElsesComment() throws Exception {
        UUID caseId = seedCase("NORTH-2026-0106", District.NORTH);
        String authorToken = TestUsers.registerAndLoginOfficer(mockMvc, "docofficer7", District.NORTH);
        Long documentId = addComment(caseId, authorToken);

        String otherToken = TestUsers.registerAndLoginOfficer(mockMvc, "docofficer8", District.NORTH);

        mockMvc.perform(delete("/api/cases/" + caseId + "/documents/" + documentId)
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void sameDistrictSupervisorCanDeleteAnyonesComment() throws Exception {
        UUID caseId = seedCase("NORTH-2026-0107", District.NORTH);
        String authorToken = TestUsers.registerAndLoginOfficer(mockMvc, "docofficer9", District.NORTH);
        Long documentId = addComment(caseId, authorToken);

        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher,
            "docsupervisor1", Role.SUPERVISOR, District.NORTH);

        mockMvc.perform(delete("/api/cases/" + caseId + "/documents/" + documentId)
                .header("Authorization", "Bearer " + supervisorToken))
            .andExpect(status().isOk());
    }

    @Test
    void crossDistrictSupervisorCannotDeleteComment() throws Exception {
        UUID caseId = seedCase("NORTH-2026-0108", District.NORTH);
        String authorToken = TestUsers.registerAndLoginOfficer(mockMvc, "docofficer10", District.NORTH);
        Long documentId = addComment(caseId, authorToken);

        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher,
            "docsupervisor2", Role.SUPERVISOR, District.EAST);

        // A supervisor outside the case's district can't even see the case (same visibility
        // rule as GET /api/cases/{id}), so this 404s before the delete-permission check runs.
        mockMvc.perform(delete("/api/cases/" + caseId + "/documents/" + documentId)
                .header("Authorization", "Bearer " + supervisorToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void adminCanDeleteAnyonesComment() throws Exception {
        UUID caseId = seedCase("NORTH-2026-0109", District.NORTH);
        String authorToken = TestUsers.registerAndLoginOfficer(mockMvc, "docofficer11", District.NORTH);
        Long documentId = addComment(caseId, authorToken);

        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher,
            "docadmin1", Role.ADMIN, District.WEST);

        mockMvc.perform(delete("/api/cases/" + caseId + "/documents/" + documentId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }

    @Test
    void documentsNotVisibleForCaseOutsideCallerAccess() throws Exception {
        UUID caseId = seedCase("EAST-2026-0104", District.EAST);
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "docofficer5", District.NORTH);

        mockMvc.perform(get("/api/cases/" + caseId + "/documents").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    private Long addComment(UUID caseId, String token) throws Exception {
        String responseBody = mockMvc.perform(multipart("/api/cases/" + caseId + "/documents")
                .param("type", "MESSAGE")
                .param("content", "A comment")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Number docIdNumber = com.jayway.jsonpath.JsonPath.read(responseBody, "$.id");
        return docIdNumber.longValue();
    }

    private UUID seedCase(String caseNumber, District district) {
        Case c = caseRepository.save(new Case(
            caseNumber, "Title for " + caseNumber, "Description", CaseStatus.OPEN,
            district, null, Instant.now()
        ));
        return c.getId();
    }
}
