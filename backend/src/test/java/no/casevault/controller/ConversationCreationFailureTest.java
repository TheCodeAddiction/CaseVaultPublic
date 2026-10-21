package no.casevault.controller;

import no.casevault.model.District;
import no.casevault.repository.UserRepository;
import no.casevault.testsupport.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression coverage for a bug where a first message that fails validation
 * (after the conversation is find-or-created but before the message itself
 * is saved) left an orphaned, message-less Conversation row -- which then
 * 500'd GET /api/conversations for both participants forever.
 *
 * Deliberately NOT @Transactional: the other controller tests roll back
 * per-test DB state, which also hides this exact bug, since findOrCreate and
 * the message save only run as genuinely separate committed transactions in
 * production (there's no @Transactional wrapping the send path). Uses unique
 * usernames so no cleanup is needed between runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConversationCreationFailureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    // Not @Transactional (see class javadoc), so this test's users would otherwise
    // persist for the rest of the suite and skew unrelated "list all users"/"count
    // users" assertions in other test classes sharing this DB. Clean them up.
    @AfterEach
    void cleanUp() {
        userRepository.findByUsernameIgnoreCase("orphansender1").ifPresent(userRepository::delete);
        userRepository.findByUsernameIgnoreCase("orphanrecipient1").ifPresent(userRepository::delete);
    }

    @Test
    void failedFirstMessageDoesNotOrphanConversationForEitherParty() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "orphansender1", District.NORTH);
        String recipientToken = TestUsers.registerAndLoginOfficer(mockMvc, "orphanrecipient1", District.NORTH);

        mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "orphanrecipient1")
                .param("body", "hello")
                .param("relatedCaseId", UUID.randomUUID().toString())
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("CASE_NOT_FOUND"));

        mockMvc.perform(get("/api/conversations").header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/conversations").header("Authorization", "Bearer " + recipientToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
