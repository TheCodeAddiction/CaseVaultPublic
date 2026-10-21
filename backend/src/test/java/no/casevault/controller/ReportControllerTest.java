package no.casevault.controller;

import no.casevault.model.CaseStatus;
import no.casevault.model.District;
import no.casevault.model.Role;
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
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    void supervisorSeesOverviewForOwnDistrict() throws Exception {
        String officerToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerReportA", Role.OFFICER, District.EAST);
        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "supervisorReportA", Role.SUPERVISOR, District.EAST);
        var officer = userRepository.findByUsernameIgnoreCase("officerReportA").orElseThrow();

        caseRepository.save(new no.casevault.model.Case("EAST-2026-0001", "Case one", "desc",
            CaseStatus.OPEN, District.EAST, officer, java.time.Instant.now()));
        caseRepository.save(new no.casevault.model.Case("EAST-2026-0002", "Case two", "desc",
            CaseStatus.CLOSED, District.EAST, null, java.time.Instant.now()));

        mockMvc.perform(get("/api/reports/district-overview").header("Authorization", "Bearer " + supervisorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.district").value("EAST"))
            .andExpect(jsonPath("$.totalCases").value(2))
            .andExpect(jsonPath("$.openCases").value(1))
            .andExpect(jsonPath("$.closedCases").value(1))
            .andExpect(jsonPath("$.officerWorkload[0].username").value("officerReportA"))
            .andExpect(jsonPath("$.officerWorkload[0].assignedCaseCount").value(1));
    }

    @Test
    void officerCannotViewDistrictOverview() throws Exception {
        String officerToken = TestUsers.registerAndLoginOfficer(mockMvc, "plainofficerReport", District.NORTH);

        mockMvc.perform(get("/api/reports/district-overview").header("Authorization", "Bearer " + officerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanViewAnyDistrictOverviewViaQueryParam() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminReportA", Role.ADMIN, District.NORTH);
        caseRepository.save(new no.casevault.model.Case("WEST-2026-0001", "West case", "desc",
            CaseStatus.OPEN, District.WEST, null, java.time.Instant.now()));

        mockMvc.perform(get("/api/reports/district-overview?district=WEST").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.district").value("WEST"))
            .andExpect(jsonPath("$.totalCases").value(1));
    }

    @Test
    void supervisorCannotOverrideDistrictViaQueryParam() throws Exception {
        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "supervisorReportB", Role.SUPERVISOR, District.SOUTH);
        caseRepository.save(new no.casevault.model.Case("WEST-2026-0002", "West case", "desc",
            CaseStatus.OPEN, District.WEST, null, java.time.Instant.now()));

        mockMvc.perform(get("/api/reports/district-overview?district=WEST").header("Authorization", "Bearer " + supervisorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.district").value("SOUTH"));
    }
}
