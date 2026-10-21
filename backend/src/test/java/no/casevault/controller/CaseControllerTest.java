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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/cases"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void officerSeesOnlyCasesInOwnDistrict() throws Exception {
        seedCase("NORTH-2026-0001", District.NORTH);
        seedCase("NORTH-2026-0002", District.NORTH);
        seedCase("EAST-2026-0001", District.EAST);

        String token = TestUsers.registerAndLoginOfficer(mockMvc, "northofficer", District.NORTH);

        mockMvc.perform(get("/api/cases").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void adminSeesAllCasesAcrossAllDistricts() throws Exception {
        seedCase("NORTH-2026-0001", District.NORTH);
        seedCase("NORTH-2026-0002", District.NORTH);
        seedCase("EAST-2026-0001", District.EAST);

        String token = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "theadmin", Role.ADMIN, District.WEST);

        mockMvc.perform(get("/api/cases").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    void createCaseAssignsToCreatorInOwnDistrictAndLogsActivity() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "creator1", District.SOUTH);

        var request = """
            {
              "title": "Stolen bicycle",
              "description": "Reported missing from a locked rack outside the library."
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/cases")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Stolen bicycle"))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.district").value("SOUTH"))
            .andExpect(jsonPath("$.assignedOfficerUsername").value("creator1"))
            .andExpect(jsonPath("$.caseNumber").value(org.hamcrest.Matchers.startsWith("SOUTH-")))
            .andReturn();

        String caseIdString = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        UUID caseId = UUID.fromString(caseIdString);

        mockMvc.perform(get("/api/cases/" + caseId + "/activity").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].action").value("CASE_CREATED"))
            .andExpect(jsonPath("$[0].actorUsername").value("creator1"));
    }

    @Test
    void createCaseRejectsBlankDescription() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "creator2", District.SOUTH);

        var request = """
            {
              "title": "Valid title",
              "description": ""
            }
            """;

        mockMvc.perform(post("/api/cases")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void getCaseReturnsDetailWhenVisible() throws Exception {
        seedCase("NORTH-2026-0010", District.NORTH);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0010"))
            .findFirst().orElseThrow().getId();

        String token = TestUsers.registerAndLoginOfficer(mockMvc, "viewer1", District.NORTH);

        mockMvc.perform(get("/api/cases/" + caseId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.caseNumber").value("NORTH-2026-0010"));
    }

    @Test
    void getCaseReturns404WhenNotVisibleToCaller() throws Exception {
        seedCase("EAST-2026-0011", District.EAST);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("EAST-2026-0011"))
            .findFirst().orElseThrow().getId();

        String token = TestUsers.registerAndLoginOfficer(mockMvc, "viewer2", District.NORTH);

        mockMvc.perform(get("/api/cases/" + caseId).header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("CASE_NOT_FOUND"));
    }

    @Test
    void updateStatusChangesStatusAndLogsActivity() throws Exception {
        seedCase("NORTH-2026-0020", District.NORTH);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0020"))
            .findFirst().orElseThrow().getId();

        String token = TestUsers.registerAndLoginOfficer(mockMvc, "statusofficer1", District.NORTH);

        mockMvc.perform(put("/api/cases/" + caseId + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"status\": \"IN_PROGRESS\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/cases/" + caseId + "/activity").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].action").value("STATUS_CHANGED"))
            .andExpect(jsonPath("$[0].details").value("Status changed from OPEN to IN_PROGRESS"));
    }

    @Test
    void updateStatusReturns404WhenCaseNotVisible() throws Exception {
        seedCase("EAST-2026-0021", District.EAST);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("EAST-2026-0021"))
            .findFirst().orElseThrow().getId();

        String token = TestUsers.registerAndLoginOfficer(mockMvc, "statusofficer2", District.NORTH);

        mockMvc.perform(put("/api/cases/" + caseId + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"status\": \"CLOSED\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void listCasesFiltersByStatus() throws Exception {
        seedCaseWithStatus("NORTH-2026-0030", District.NORTH, CaseStatus.OPEN);
        seedCaseWithStatus("NORTH-2026-0031", District.NORTH, CaseStatus.CLOSED);

        String token = TestUsers.registerAndLoginOfficer(mockMvc, "searchofficer1", District.NORTH);

        mockMvc.perform(get("/api/cases?status=CLOSED").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].caseNumber").value("NORTH-2026-0031"));
    }

    @Test
    void listCasesFiltersByKeywordInTitleOrDescription() throws Exception {
        caseRepository.save(new Case(
            "NORTH-2026-0040", "Stolen bicycle", "Missing from a rack outside the library.",
            CaseStatus.OPEN, District.NORTH, null, Instant.now()
        ));
        caseRepository.save(new Case(
            "NORTH-2026-0041", "Vehicle break-in", "Window smashed overnight.",
            CaseStatus.OPEN, District.NORTH, null, Instant.now()
        ));

        String token = TestUsers.registerAndLoginOfficer(mockMvc, "searchofficer2", District.NORTH);

        mockMvc.perform(get("/api/cases?q=bicycle").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].caseNumber").value("NORTH-2026-0040"));
    }

    @Test
    void listCasesSearchStillRespectsVisibilityScoping() throws Exception {
        seedCase("EAST-2026-0050", District.EAST);

        String token = TestUsers.registerAndLoginOfficer(mockMvc, "searchofficer3", District.NORTH);

        mockMvc.perform(get("/api/cases?q=Title").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void supervisorAssignsCaseToOfficerInSameDistrict() throws Exception {
        seedCase("NORTH-2026-0060", District.NORTH);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0060")).findFirst().orElseThrow().getId();

        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "assignee1", Role.OFFICER, District.NORTH);
        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "assignsupervisor1", Role.SUPERVISOR, District.NORTH);

        mockMvc.perform(put("/api/cases/" + caseId + "/assignee")
                .header("Authorization", "Bearer " + supervisorToken)
                .contentType("application/json")
                .content("{\"officerUsername\": \"assignee1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignedOfficerUsername").value("assignee1"));
    }

    @Test
    void officerCannotAssignCases() throws Exception {
        seedCase("NORTH-2026-0061", District.NORTH);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0061")).findFirst().orElseThrow().getId();

        String officerToken = TestUsers.registerAndLoginOfficer(mockMvc, "assignee2", District.NORTH);

        mockMvc.perform(put("/api/cases/" + caseId + "/assignee")
                .header("Authorization", "Bearer " + officerToken)
                .contentType("application/json")
                .content("{\"officerUsername\": \"assignee2\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void assigningToOfficerInWrongDistrictIsRejected() throws Exception {
        seedCase("NORTH-2026-0062", District.NORTH);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0062")).findFirst().orElseThrow().getId();

        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "eastofficer1", Role.OFFICER, District.EAST);
        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "assignsupervisor2", Role.SUPERVISOR, District.NORTH);

        mockMvc.perform(put("/api/cases/" + caseId + "/assignee")
                .header("Authorization", "Bearer " + supervisorToken)
                .contentType("application/json")
                .content("{\"officerUsername\": \"eastofficer1\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_CASE_OPERATION"));
    }

    @Test
    void supervisorSharesCaseAndOtherDistrictCanThenSeeIt() throws Exception {
        seedCase("NORTH-2026-0070", District.NORTH);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0070")).findFirst().orElseThrow().getId();

        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "sharesupervisor1", Role.SUPERVISOR, District.NORTH);

        mockMvc.perform(post("/api/cases/" + caseId + "/shares")
                .header("Authorization", "Bearer " + supervisorToken)
                .contentType("application/json")
                .content("{\"district\": \"EAST\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sharedWithDistrict").value("EAST"));

        String eastOfficerToken = TestUsers.registerAndLoginOfficer(mockMvc, "eastviewer1", District.EAST);

        mockMvc.perform(get("/api/cases/" + caseId).header("Authorization", "Bearer " + eastOfficerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.caseNumber").value("NORTH-2026-0070"));

        mockMvc.perform(get("/api/cases").header("Authorization", "Bearer " + eastOfficerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.caseNumber == 'NORTH-2026-0070')]").exists());
    }

    @Test
    void unsharedCaseRemainsInvisibleToOtherDistricts() throws Exception {
        seedCase("NORTH-2026-0071", District.NORTH);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0071")).findFirst().orElseThrow().getId();

        String eastOfficerToken = TestUsers.registerAndLoginOfficer(mockMvc, "eastviewer2", District.EAST);

        mockMvc.perform(get("/api/cases/" + caseId).header("Authorization", "Bearer " + eastOfficerToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void officerCannotShareCases() throws Exception {
        seedCase("NORTH-2026-0072", District.NORTH);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0072")).findFirst().orElseThrow().getId();

        String officerToken = TestUsers.registerAndLoginOfficer(mockMvc, "shareofficer1", District.NORTH);

        mockMvc.perform(post("/api/cases/" + caseId + "/shares")
                .header("Authorization", "Bearer " + officerToken)
                .contentType("application/json")
                .content("{\"district\": \"EAST\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void supervisorSharesCaseWithASpecificUserInAnotherDistrict() throws Exception {
        seedCase("NORTH-2026-0073", District.NORTH);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0073")).findFirst().orElseThrow().getId();

        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "sharesupervisor2", Role.SUPERVISOR, District.NORTH);
        String eastOfficerToken = TestUsers.registerAndLoginOfficer(mockMvc, "eastviewer3", District.EAST);

        mockMvc.perform(post("/api/cases/" + caseId + "/shares")
                .header("Authorization", "Bearer " + supervisorToken)
                .contentType("application/json")
                .content("{\"username\": \"eastviewer3\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sharedWithUsername").value("eastviewer3"));

        mockMvc.perform(get("/api/cases/" + caseId).header("Authorization", "Bearer " + eastOfficerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.caseNumber").value("NORTH-2026-0073"));
    }

    @Test
    void userShareDoesNotGrantVisibilityToOtherUsersInThatDistrict() throws Exception {
        seedCase("NORTH-2026-0074", District.NORTH);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0074")).findFirst().orElseThrow().getId();

        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "sharesupervisor3", Role.SUPERVISOR, District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "eastviewer4", District.EAST);
        String otherEastOfficerToken = TestUsers.registerAndLoginOfficer(mockMvc, "eastviewer5", District.EAST);

        mockMvc.perform(post("/api/cases/" + caseId + "/shares")
                .header("Authorization", "Bearer " + supervisorToken)
                .contentType("application/json")
                .content("{\"username\": \"eastviewer4\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cases/" + caseId).header("Authorization", "Bearer " + otherEastOfficerToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void shareRequestRejectsBothDistrictAndUsername() throws Exception {
        seedCase("NORTH-2026-0075", District.NORTH);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0075")).findFirst().orElseThrow().getId();

        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "sharesupervisor4", Role.SUPERVISOR, District.NORTH);

        mockMvc.perform(post("/api/cases/" + caseId + "/shares")
                .header("Authorization", "Bearer " + supervisorToken)
                .contentType("application/json")
                .content("{\"district\": \"EAST\", \"username\": \"someone\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_CASE_OPERATION"));
    }

    @Test
    void supervisorRevokesADistrictShareAndVisibilityIsRemoved() throws Exception {
        seedCase("NORTH-2026-0076", District.NORTH);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0076")).findFirst().orElseThrow().getId();

        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "sharesupervisor5", Role.SUPERVISOR, District.NORTH);

        MvcResult created = mockMvc.perform(post("/api/cases/" + caseId + "/shares")
                .header("Authorization", "Bearer " + supervisorToken)
                .contentType("application/json")
                .content("{\"district\": \"EAST\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        String shareId = created.getResponse().getContentAsString().split("\"id\":")[1].split(",")[0];

        String eastOfficerToken = TestUsers.registerAndLoginOfficer(mockMvc, "eastviewer6", District.EAST);
        mockMvc.perform(get("/api/cases/" + caseId).header("Authorization", "Bearer " + eastOfficerToken))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/cases/" + caseId + "/shares/" + shareId)
                .header("Authorization", "Bearer " + supervisorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Share removed."));

        mockMvc.perform(get("/api/cases/" + caseId).header("Authorization", "Bearer " + eastOfficerToken))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/cases/" + caseId + "/shares").header("Authorization", "Bearer " + supervisorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void officerCannotRevokeShares() throws Exception {
        seedCase("NORTH-2026-0077", District.NORTH);
        UUID caseId = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0077")).findFirst().orElseThrow().getId();

        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "sharesupervisor6", Role.SUPERVISOR, District.NORTH);
        MvcResult created = mockMvc.perform(post("/api/cases/" + caseId + "/shares")
                .header("Authorization", "Bearer " + supervisorToken)
                .contentType("application/json")
                .content("{\"district\": \"EAST\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        String shareId = created.getResponse().getContentAsString().split("\"id\":")[1].split(",")[0];

        String officerToken = TestUsers.registerAndLoginOfficer(mockMvc, "shareofficer2", District.NORTH);
        mockMvc.perform(delete("/api/cases/" + caseId + "/shares/" + shareId)
                .header("Authorization", "Bearer " + officerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void revokingAShareThatDoesNotBelongToTheCaseReturns404() throws Exception {
        seedCase("NORTH-2026-0078", District.NORTH);
        seedCase("NORTH-2026-0079", District.NORTH);
        UUID caseIdA = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0078")).findFirst().orElseThrow().getId();
        UUID caseIdB = caseRepository.findAll().stream()
            .filter(c -> c.getCaseNumber().equals("NORTH-2026-0079")).findFirst().orElseThrow().getId();

        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "sharesupervisor7", Role.SUPERVISOR, District.NORTH);
        MvcResult created = mockMvc.perform(post("/api/cases/" + caseIdA + "/shares")
                .header("Authorization", "Bearer " + supervisorToken)
                .contentType("application/json")
                .content("{\"district\": \"EAST\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        String shareId = created.getResponse().getContentAsString().split("\"id\":")[1].split(",")[0];

        mockMvc.perform(delete("/api/cases/" + caseIdB + "/shares/" + shareId)
                .header("Authorization", "Bearer " + supervisorToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("CASE_SHARE_NOT_FOUND"));
    }

    @Test
    void exportReturnsCsvOfVisibleCases() throws Exception {
        seedCase("NORTH-2026-0080", District.NORTH);
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "exportofficer1", District.NORTH);

        MvcResult result = mockMvc.perform(get("/api/cases/export").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

        String csv = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(csv).contains("Case Number,Title,Status");
        org.assertj.core.api.Assertions.assertThat(csv).contains("NORTH-2026-0080");
    }

    @Test
    void exportNeutralizesFormulaLikeFieldsToPreventCsvInjection() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "exportofficer2", District.NORTH);

        mockMvc.perform(post("/api/cases")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"title\": \"=cmd|'/c calc'!A1\", \"description\": \"Formula-like title.\"}"))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/cases/export").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

        String csv = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(csv).contains("\"'=cmd|'/c calc'!A1\"");
    }

    @Test
    void listCasesPaginatesResults() throws Exception {
        for (int i = 0; i < 5; i++) {
            seedCase("NORTH-2026-01" + i, District.NORTH);
        }
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "pageofficer1", District.NORTH);

        mockMvc.perform(get("/api/cases?page=0&size=2").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void statsReflectsOnlyCasesVisibleToCaller() throws Exception {
        seedCaseWithStatus("NORTH-2026-0090", District.NORTH, CaseStatus.OPEN);
        seedCaseWithStatus("NORTH-2026-0091", District.NORTH, CaseStatus.IN_PROGRESS);
        seedCaseWithStatus("NORTH-2026-0092", District.NORTH, CaseStatus.CLOSED);
        seedCase("EAST-2026-0090", District.EAST);

        String token = TestUsers.registerAndLoginOfficer(mockMvc, "statsofficer1", District.NORTH);

        mockMvc.perform(get("/api/cases/stats").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCases").value(3))
            .andExpect(jsonPath("$.openCases").value(1))
            .andExpect(jsonPath("$.inProgressCases").value(1))
            .andExpect(jsonPath("$.closedCases").value(1))
            .andExpect(jsonPath("$.recentCases.length()").value(3));
    }

    @Test
    void statsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/cases/stats"))
            .andExpect(status().isUnauthorized());
    }

    private void seedCase(String caseNumber, District district) {
        caseRepository.save(new Case(
            caseNumber, "Title for " + caseNumber, "Description", CaseStatus.OPEN,
            district, null, Instant.now()
        ));
    }

    private void seedCaseWithStatus(String caseNumber, District district, CaseStatus status) {
        caseRepository.save(new Case(
            caseNumber, "Title for " + caseNumber, "Description", status,
            district, null, Instant.now()
        ));
    }
}
