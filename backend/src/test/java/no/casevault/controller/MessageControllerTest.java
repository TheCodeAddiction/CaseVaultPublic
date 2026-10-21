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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private CaseRepository caseRepository;

    private static String extractField(String json, String field) {
        var matcher = Pattern.compile("\"" + field + "\":\"([^\"]+)\"").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Field not found in response: " + field + " -- " + json);
        }
        return matcher.group(1);
    }

    @Test
    void sendsMessage() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "msgsender1", District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "msgrecipient1", District.NORTH);

        mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "msgrecipient1")
                .param("body", "Can you cover my shift?")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.senderUsername").value("msgsender1"))
            .andExpect(jsonPath("$.recipientUsername").value("msgrecipient1"))
            .andExpect(jsonPath("$.body").value("Can you cover my shift?"))
            .andExpect(jsonPath("$.conversationId").exists())
            .andExpect(jsonPath("$.readAt").doesNotExist());
    }

    @Test
    void sendingTwoMessagesToSameRecipientReusesTheSameConversation() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "msgsender11", District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "msgrecipient11", District.NORTH);

        MvcResult first = mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "msgrecipient11")
                .param("body", "first")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isCreated())
            .andReturn();

        MvcResult second = mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "msgrecipient11")
                .param("body", "second")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isCreated())
            .andReturn();

        String firstConversationId = extractField(first.getResponse().getContentAsString(), "conversationId");
        String secondConversationId = extractField(second.getResponse().getContentAsString(), "conversationId");

        org.junit.jupiter.api.Assertions.assertEquals(firstConversationId, secondConversationId);
    }

    @Test
    void replyingInTheOppositeDirectionReusesTheSameConversation() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "msgsender13", District.NORTH);
        String recipientToken = TestUsers.registerAndLoginOfficer(mockMvc, "msgrecipient13", District.NORTH);

        MvcResult first = mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "msgrecipient13")
                .param("body", "first")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isCreated())
            .andReturn();

        // Reply comes from the *other* user this time -- this is the direction that
        // actually exercises canonicalization in ConversationService#findOrCreate,
        // since findByUserAAndUserB(A, B) alone would still match on a same-direction
        // resend regardless of whether canonicalization worked.
        MvcResult second = mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "msgsender13")
                .param("body", "reply from the other side")
                .header("Authorization", "Bearer " + recipientToken))
            .andExpect(status().isCreated())
            .andReturn();

        String firstConversationId = extractField(first.getResponse().getContentAsString(), "conversationId");
        String secondConversationId = extractField(second.getResponse().getContentAsString(), "conversationId");

        org.junit.jupiter.api.Assertions.assertEquals(firstConversationId, secondConversationId);
    }

    @Test
    void sendingMessageToSelfIsRejected() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "msgsender12", District.NORTH);

        mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "msgsender12")
                .param("body", "hello me")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_MESSAGE"));
    }

    @Test
    void sendingToUnknownRecipientReturns404() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "msgsender2", District.NORTH);

        mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "does-not-exist")
                .param("body", "hello")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    void sendingWithBlankBodyIsRejected() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "msgsender6", District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "msgrecipient6", District.NORTH);

        mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "msgrecipient6")
                .param("body", "   ")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_MESSAGE"));
    }

    @Test
    void sendingAnOverlongBodyIsRejected() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "msgsender13", District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "msgrecipient13", District.NORTH);
        String tooLong = "x".repeat(4001);

        mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "msgrecipient13")
                .param("body", tooLong)
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_MESSAGE"));
    }

    @Test
    void unreadCountReflectsUnreadMessages() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "msgsender3", District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "msgrecipient3", District.NORTH);
        String recipientToken = TestUsers.login(mockMvc, "msgrecipient3");

        mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "msgrecipient3")
                .param("body", "hello")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/messages/unread-count").header("Authorization", "Bearer " + recipientToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    void sendMessageRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "whoever")
                .param("body", "hello"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void sendsMessageWithLinkedCaseVisibleToSender() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "msgsender7", District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "msgrecipient7", District.NORTH);
        var sender = userRepository.findByUsernameIgnoreCase("msgsender7").orElseThrow();

        var savedCase = caseRepository.save(new no.casevault.model.Case("NORTH-2026-8001", "Shared case", "desc",
            CaseStatus.OPEN, District.NORTH, sender, Instant.now()));

        mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "msgrecipient7")
                .param("body", "Check this out")
                .param("relatedCaseId", savedCase.getId().toString())
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.relatedCaseId").value(savedCase.getId().toString()))
            .andExpect(jsonPath("$.relatedCaseNumber").value("NORTH-2026-8001"))
            .andExpect(jsonPath("$.relatedCaseTitle").value("Shared case"));
    }

    @Test
    void sendingMessageWithCaseTheSenderCannotSeeReturns404() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "msgsender8", District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "msgrecipient8", District.NORTH);

        var hiddenCase = caseRepository.save(new no.casevault.model.Case("EAST-2026-8002", "Hidden case", "desc",
            CaseStatus.OPEN, District.EAST, null, Instant.now()));

        mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "msgrecipient8")
                .param("body", "Check this out")
                .param("relatedCaseId", hiddenCase.getId().toString())
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("CASE_NOT_FOUND"));
    }

    @Test
    void sendsMessageWithAttachmentAndBothPartiesCanDownloadIt() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "msgsender9", District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "msgrecipient9", District.NORTH);
        String recipientToken = TestUsers.login(mockMvc, "msgrecipient9");
        var attachment = new MockMultipartFile("attachment", "notes.txt", "text/plain", "hello attachment".getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/messages")
                .file(attachment)
                .param("recipientUsername", "msgrecipient9")
                .param("body", "See attached")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.attachmentFilename").value("notes.txt"))
            .andReturn();

        String messageId = extractField(result.getResponse().getContentAsString(), "id");

        mockMvc.perform(get("/api/messages/" + messageId + "/attachment").header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes("hello attachment".getBytes()));

        mockMvc.perform(get("/api/messages/" + messageId + "/attachment").header("Authorization", "Bearer " + recipientToken))
            .andExpect(status().isOk());
    }

    @Test
    void outsiderCannotDownloadMessageAttachment() throws Exception {
        String senderToken = TestUsers.registerAndLoginOfficer(mockMvc, "msgsender10", District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "msgrecipient10", District.NORTH);
        String otherToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "msgoutsider10", Role.OFFICER, District.EAST);
        var attachment = new MockMultipartFile("attachment", "notes.txt", "text/plain", "hello attachment".getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/messages")
                .file(attachment)
                .param("recipientUsername", "msgrecipient10")
                .param("body", "See attached")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isCreated())
            .andReturn();

        String messageId = extractField(result.getResponse().getContentAsString(), "id");

        mockMvc.perform(get("/api/messages/" + messageId + "/attachment").header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden());
    }
}
