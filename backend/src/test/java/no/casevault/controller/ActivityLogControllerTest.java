package no.casevault.controller;

import no.casevault.model.District;
import no.casevault.testsupport.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ActivityLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void recordsAnEntryAttributedToTheCallingUser() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "logofficer1", District.NORTH);
        String entityId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/activity-log")
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .content("""
                    {"entityType": "CASE", "entityId": "%s", "action": "COMMENT_DELETED", "details": "Removed a comment"}
                    """.formatted(entityId)))
            .andExpect(status().isCreated());
    }

    @Test
    void actorIsAlwaysDerivedFromTheCallersOwnTokenNotClientInput() throws Exception {
        String officerToken = TestUsers.registerAndLoginOfficer(mockMvc, "logofficer2", District.NORTH);
        String entityId = UUID.randomUUID().toString();

        // No "actor"/"username" field exists on the request DTO at all -- the endpoint has
        // nothing for a client to spoof; the actor always comes from the caller's own JWT.
        mockMvc.perform(post("/api/activity-log")
                .contentType("application/json")
                .header("Authorization", "Bearer " + officerToken)
                .content("""
                    {"entityType": "CASE", "entityId": "%s", "action": "COMMENT_DELETED", "details": "Removed a comment", "actor": "someoneelse"}
                    """.formatted(entityId)))
            .andExpect(status().isCreated());
    }

    @Test
    void rejectsMissingFields() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "logofficer3", District.NORTH);

        mockMvc.perform(post("/api/activity-log")
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/activity-log")
                .contentType("application/json")
                .content("""
                    {"entityType": "CASE", "entityId": "1", "action": "COMMENT_DELETED"}
                    """))
            .andExpect(status().isUnauthorized());
    }
}
