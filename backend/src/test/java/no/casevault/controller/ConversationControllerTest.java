package no.casevault.controller;

import no.casevault.model.District;
import no.casevault.model.Role;
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

import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    private static String extractField(String json, String field) {
        var matcher = Pattern.compile("\"" + field + "\":\"([^\"]+)\"").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Field not found in response: " + field + " -- " + json);
        }
        return matcher.group(1);
    }

    @Test
    void listsConversationWithPreviewAndUnreadCount() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "convsender1", District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "convrecipient1", District.NORTH);
        String recipientToken = TestUsers.login(mockMvc, "convrecipient1");

        mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "convrecipient1")
                .param("body", "hello there")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/conversations").header("Authorization", "Bearer " + recipientToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].otherParticipantUsername").value("convsender1"))
            .andExpect(jsonPath("$[0].lastMessageBody").value("hello there"))
            .andExpect(jsonPath("$[0].unreadCount").value(1));

        mockMvc.perform(get("/api/conversations").header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].otherParticipantUsername").value("convrecipient1"))
            .andExpect(jsonPath("$[0].unreadCount").value(0));
    }

    @Test
    void repliesWithinAConversationAndFetchesFullThread() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "convsender2", District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "convrecipient2", District.NORTH);
        String recipientToken = TestUsers.login(mockMvc, "convrecipient2");

        MvcResult created = mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "convrecipient2")
                .param("body", "first message")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isCreated())
            .andReturn();

        String conversationId = extractField(created.getResponse().getContentAsString(), "conversationId");

        mockMvc.perform(multipart("/api/conversations/" + conversationId + "/messages")
                .param("body", "reply message")
                .header("Authorization", "Bearer " + recipientToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.senderUsername").value("convrecipient2"))
            .andExpect(jsonPath("$.conversationId").value(conversationId));

        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].body").value("first message"))
            .andExpect(jsonPath("$[1].body").value("reply message"));
    }

    @Test
    void markingConversationReadClearsUnreadCount() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "convsender3", District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "convrecipient3", District.NORTH);
        String recipientToken = TestUsers.login(mockMvc, "convrecipient3");

        MvcResult created = mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "convrecipient3")
                .param("body", "unread message")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isCreated())
            .andReturn();

        String conversationId = extractField(created.getResponse().getContentAsString(), "conversationId");

        mockMvc.perform(post("/api/conversations/" + conversationId + "/read")
                .header("Authorization", "Bearer " + recipientToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].readAt").exists());

        mockMvc.perform(get("/api/conversations").header("Authorization", "Bearer " + recipientToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].unreadCount").value(0));
    }

    @Test
    void nonParticipantCannotAccessConversation() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "convsender4", District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "convrecipient4", District.NORTH);
        String otherToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "convoutsider4", Role.OFFICER, District.EAST);

        MvcResult created = mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "convrecipient4")
                .param("body", "private")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isCreated())
            .andReturn();

        String conversationId = extractField(created.getResponse().getContentAsString(), "conversationId");

        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/conversations/" + conversationId + "/messages")
                .param("body", "sneaky reply")
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/conversations/" + conversationId + "/read")
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void unknownConversationReturns404() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "convsender5", District.NORTH);

        mockMvc.perform(get("/api/conversations/" + java.util.UUID.randomUUID() + "/messages")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("CONVERSATION_NOT_FOUND"));
    }
}
