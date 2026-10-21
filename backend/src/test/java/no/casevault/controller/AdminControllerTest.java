package no.casevault.controller;

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
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers GET /api/admin/users (officer lookup and full user listing),
 * POST /api/admin/users (admin account creation), and the own-profile
 * endpoints under /api/admin/users/me, plus role/district management,
 * deactivation and deletion of existing accounts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerTest {

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
    private PasswordHasher passwordHasher;

    @Autowired
    private CaseRepository caseRepository;

    @Test
    void supervisorListsOfficersInOwnDistrict() throws Exception {
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerA", Role.OFFICER, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerB", Role.OFFICER, District.EAST);
        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "supervisorA", Role.SUPERVISOR, District.NORTH);

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + supervisorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].username").value("officerA"));
    }

    @Test
    void officerCannotListUsers() throws Exception {
        String officerToken = TestUsers.registerAndLoginOfficer(mockMvc, "plainofficer", District.NORTH);

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + officerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminListsAllUsersAcrossDistricts() throws Exception {
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerC", Role.OFFICER, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "supervisorC", Role.SUPERVISOR, District.EAST);
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminC", Role.ADMIN, District.WEST);

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void adminCreatesUserWithAnyRoleAndDistrict() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminD", Role.ADMIN, District.NORTH);

        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .content("""
                    {"fullName": "New Supervisor", "username": "newsupervisor1", "email": "newsupervisor1@example.test", "password": "a brand new password", "role": "SUPERVISOR", "district": "SOUTH"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("newsupervisor1"))
            .andExpect(jsonPath("$.role").value("SUPERVISOR"))
            .andExpect(jsonPath("$.district").value("SOUTH"));

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"newsupervisor1\", \"password\": \"a brand new password\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void nonAdminCannotCreateUser() throws Exception {
        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "supervisorD", Role.SUPERVISOR, District.NORTH);

        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + supervisorToken)
                .contentType("application/json")
                .content("""
                    {"fullName": "New Officer", "username": "newofficer1", "email": "newofficer1@example.test", "password": "a brand new password", "role": "OFFICER", "district": "NORTH"}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void createUserRejectsDuplicateUsername() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminE", Role.ADMIN, District.NORTH);

        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .content("""
                    {"fullName": "Duplicate", "username": "adminE", "email": "dup@example.test", "password": "a brand new password", "role": "OFFICER", "district": "NORTH"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("USERNAME_TAKEN"));
    }

    @Test
    void createUserRejectsTooShortPassword() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminF", Role.ADMIN, District.NORTH);

        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .content("""
                    {"fullName": "Short Pw", "username": "shortpwuser", "email": "shortpwuser@example.test", "password": "short", "role": "OFFICER", "district": "NORTH"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void anyRoleCanViewOwnProfile() throws Exception {
        String officerToken = TestUsers.registerAndLoginOfficer(mockMvc, "profileofficer", District.NORTH);

        mockMvc.perform(get("/api/admin/users/me").header("Authorization", "Bearer " + officerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("profileofficer"))
            .andExpect(jsonPath("$.district").value("NORTH"))
            .andExpect(jsonPath("$.role").value("OFFICER"));
    }

    @Test
    void viewingOwnProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updatesOwnFullNameAndEmail() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "acctupdater1", District.NORTH);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/users/me")
                .param("fullName", "New Name")
                .param("email", "new-email@example.test")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("New Name"))
            .andExpect(jsonPath("$.email").value("new-email@example.test"))
            .andExpect(jsonPath("$.role").value("OFFICER"));
    }

    @Test
    void changePasswordSucceedsWithCorrectCurrentPassword() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "pwchanger1", District.NORTH);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/users/me")
                .param("currentPassword", TestUsers.DEFAULT_PASSWORD)
                .param("newPassword", "a brand new password")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("pwchanger1"));

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"pwchanger1\", \"password\": \"a brand new password\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "pwchanger2", District.NORTH);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/users/me")
                .param("currentPassword", "totally wrong")
                .param("newPassword", "a brand new password")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void changePasswordRejectsTooShortNewPassword() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "pwchanger3", District.NORTH);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/users/me")
                .param("currentPassword", TestUsers.DEFAULT_PASSWORD)
                .param("newPassword", "short")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void changingPasswordWithoutCurrentPasswordIsRejected() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "pwchanger4", District.NORTH);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/users/me")
                .param("newPassword", "a brand new password")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void uploadsProfilePictureAndReflectsItOnProfile() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "pictureofficer1", District.NORTH);
        var file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", withJpegMagic("fake bytes"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/users/me")
                .file(file)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasProfilePicture").value(true));

        mockMvc.perform(get("/api/admin/users/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasProfilePicture").value(true));
    }

    @Test
    void rejectsDisallowedProfilePictureFileType() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "pictureofficer2", District.NORTH);
        var file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "x".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/users/me")
                .file(file)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_FILE"));
    }

    @Test
    void rejectsNonImageProfilePictureEvenIfOtherwiseAllowedForCaseFiles() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "pictureofficer3", District.NORTH);
        var file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/users/me")
                .file(file)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_FILE"));
    }

    @Test
    void viewsAnUploadedProfilePicture() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "pictureofficer4", District.NORTH);
        var file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", withJpegMagic("fake bytes"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/users/me").file(file).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        Long userId = userRepository.findByUsernameIgnoreCase("pictureofficer4").orElseThrow().getId();

        mockMvc.perform(get("/api/admin/users/" + userId + "/picture").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/jpeg"))
            .andExpect(content().bytes(withJpegMagic("fake bytes")));
    }

    @Test
    void viewingPictureOfUserWithNoneReturns404() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "pictureofficer5", District.NORTH);
        Long userId = userRepository.findByUsernameIgnoreCase("pictureofficer5").orElseThrow().getId();

        mockMvc.perform(get("/api/admin/users/" + userId + "/picture").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("PROFILE_PICTURE_NOT_FOUND"));
    }

    @Test
    void adminChangesUserRole() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminG", Role.ADMIN, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerG", Role.OFFICER, District.NORTH);
        Long officerId = userRepository.findByUsernameIgnoreCase("officerG").orElseThrow().getId();

        mockMvc.perform(put("/api/admin/users/" + officerId + "/role")
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .content("{\"role\": \"SUPERVISOR\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("SUPERVISOR"));
    }

    @Test
    void nonAdminCannotChangeUserRole() throws Exception {
        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "supervisorG", Role.SUPERVISOR, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerH", Role.OFFICER, District.NORTH);
        Long officerId = userRepository.findByUsernameIgnoreCase("officerH").orElseThrow().getId();

        mockMvc.perform(put("/api/admin/users/" + officerId + "/role")
                .header("Authorization", "Bearer " + supervisorToken)
                .contentType("application/json")
                .content("{\"role\": \"ADMIN\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void changeRoleReturns404ForUnknownUser() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminH", Role.ADMIN, District.NORTH);

        mockMvc.perform(put("/api/admin/users/999999/role")
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .content("{\"role\": \"SUPERVISOR\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    void adminChangesUserDistrict() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminI", Role.ADMIN, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerI", Role.OFFICER, District.NORTH);
        Long officerId = userRepository.findByUsernameIgnoreCase("officerI").orElseThrow().getId();

        mockMvc.perform(put("/api/admin/users/" + officerId + "/district")
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .content("{\"district\": \"WEST\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.district").value("WEST"));
    }

    @Test
    void nonAdminCannotChangeUserDistrict() throws Exception {
        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "supervisorI", Role.SUPERVISOR, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerJ", Role.OFFICER, District.NORTH);
        Long officerId = userRepository.findByUsernameIgnoreCase("officerJ").orElseThrow().getId();

        mockMvc.perform(put("/api/admin/users/" + officerId + "/district")
                .header("Authorization", "Bearer " + supervisorToken)
                .contentType("application/json")
                .content("{\"district\": \"EAST\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminDeactivatesUserAndTheyCanNoLongerLogIn() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminJ", Role.ADMIN, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerK", Role.OFFICER, District.NORTH);
        Long officerId = userRepository.findByUsernameIgnoreCase("officerK").orElseThrow().getId();

        mockMvc.perform(post("/api/admin/users/" + officerId + "/deactivate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"officerK\", \"password\": \"" + TestUsers.DEFAULT_PASSWORD + "\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void nonAdminCannotDeactivateUser() throws Exception {
        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "supervisorJ", Role.SUPERVISOR, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerL", Role.OFFICER, District.NORTH);
        Long officerId = userRepository.findByUsernameIgnoreCase("officerL").orElseThrow().getId();

        mockMvc.perform(post("/api/admin/users/" + officerId + "/deactivate")
                .header("Authorization", "Bearer " + supervisorToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminReactivatesADeactivatedUser() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminK", Role.ADMIN, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerM", Role.OFFICER, District.NORTH);
        Long officerId = userRepository.findByUsernameIgnoreCase("officerM").orElseThrow().getId();

        mockMvc.perform(post("/api/admin/users/" + officerId + "/deactivate").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/users/" + officerId + "/reactivate").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\": \"officerM\", \"password\": \"" + TestUsers.DEFAULT_PASSWORD + "\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void nonAdminCannotReactivateUser() throws Exception {
        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "supervisorK", Role.SUPERVISOR, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerN", Role.OFFICER, District.NORTH);
        Long officerId = userRepository.findByUsernameIgnoreCase("officerN").orElseThrow().getId();

        mockMvc.perform(post("/api/admin/users/" + officerId + "/reactivate").header("Authorization", "Bearer " + supervisorToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminDeletesAUserWithNoHistory() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminL", Role.ADMIN, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerO", Role.OFFICER, District.NORTH);
        Long officerId = userRepository.findByUsernameIgnoreCase("officerO").orElseThrow().getId();

        mockMvc.perform(delete("/api/admin/users/" + officerId).header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists());

        assertThat(userRepository.findById(officerId)).isEmpty();
    }

    @Test
    void nonAdminCannotDeleteUser() throws Exception {
        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "supervisorL", Role.SUPERVISOR, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerP", Role.OFFICER, District.NORTH);
        Long officerId = userRepository.findByUsernameIgnoreCase("officerP").orElseThrow().getId();

        mockMvc.perform(delete("/api/admin/users/" + officerId).header("Authorization", "Bearer " + supervisorToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void cannotDeleteAUserWithCaseHistory() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminM", Role.ADMIN, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerQ", Role.OFFICER, District.NORTH);
        var officer = userRepository.findByUsernameIgnoreCase("officerQ").orElseThrow();

        caseRepository.save(new no.casevault.model.Case("NORTH-2026-9001", "Test case", "desc",
            no.casevault.model.CaseStatus.OPEN,
            District.NORTH, officer, java.time.Instant.now()));

        mockMvc.perform(delete("/api/admin/users/" + officer.getId()).header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CANNOT_DELETE_USER"));
    }

    @Test
    void cannotDeleteAUserWhoHasAConversation() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminN", Role.ADMIN, District.NORTH);
        String senderToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerR", Role.OFFICER, District.NORTH);
        TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "officerS", Role.OFFICER, District.NORTH);
        var recipient = userRepository.findByUsernameIgnoreCase("officerS").orElseThrow();

        mockMvc.perform(multipart("/api/messages")
                .param("recipientUsername", "officerS")
                .param("body", "hello")
                .header("Authorization", "Bearer " + senderToken))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/admin/users/" + recipient.getId()).header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CANNOT_DELETE_USER"));
    }

    @Test
    void userDeletesTheirOwnAccountWithCorrectPassword() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "selfdelete1", District.NORTH);

        mockMvc.perform(delete("/api/admin/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"currentPassword\": \"" + TestUsers.DEFAULT_PASSWORD + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists());

        assertThat(userRepository.findByUsernameIgnoreCase("selfdelete1")).isEmpty();
    }

    @Test
    void selfDeleteRejectsWrongPassword() throws Exception {
        String token = TestUsers.registerAndLoginOfficer(mockMvc, "selfdelete2", District.NORTH);

        mockMvc.perform(delete("/api/admin/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"currentPassword\": \"totally wrong\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));

        assertThat(userRepository.findByUsernameIgnoreCase("selfdelete2")).isPresent();
    }

    @Test
    void adminViewsAUsersActivity() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminN", Role.ADMIN, District.NORTH);
        String officerToken = TestUsers.registerAndLoginOfficer(mockMvc, "officerR", District.NORTH);
        Long officerId = userRepository.findByUsernameIgnoreCase("officerR").orElseThrow().getId();

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/users/me")
                .param("currentPassword", TestUsers.DEFAULT_PASSWORD)
                .param("newPassword", "a brand new password")
                .header("Authorization", "Bearer " + officerToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/users/" + officerId + "/activity").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].action").value("ACCOUNT_UPDATED"))
            .andExpect(jsonPath("$[0].actorUsername").value("officerR"));
    }

    @Test
    void nonAdminCannotViewAUsersActivity() throws Exception {
        String supervisorToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "supervisorM", Role.SUPERVISOR, District.NORTH);
        TestUsers.registerAndLoginOfficer(mockMvc, "officerS", District.NORTH);
        Long officerId = userRepository.findByUsernameIgnoreCase("officerS").orElseThrow().getId();

        mockMvc.perform(get("/api/admin/users/" + officerId + "/activity").header("Authorization", "Bearer " + supervisorToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void viewingActivityForUnknownUserReturns404() throws Exception {
        String adminToken = TestUsers.createAndLoginUser(mockMvc, userRepository, passwordHasher, "adminO", Role.ADMIN, District.NORTH);

        mockMvc.perform(get("/api/admin/users/999999/activity").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }
}
