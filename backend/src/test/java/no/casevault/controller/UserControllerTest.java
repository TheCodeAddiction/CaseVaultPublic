package no.casevault.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void meWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void meWithInvalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer not-a-real-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void meWithValidTokenReturnsCurrentUser() throws Exception {
        registerUser("tvik", "correct horse battery staple", "Tone Vik", "EAST");
        String token = loginAndGetToken("tvik", "correct horse battery staple");

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("tvik"))
            .andExpect(jsonPath("$.fullName").value("Tone Vik"))
            .andExpect(jsonPath("$.district").value("EAST"))
            .andExpect(jsonPath("$.role").value("OFFICER"));
    }

    @Test
    void searchFindsMatchesByUsernameOrFullNameExcludingSelf() throws Exception {
        registerUser("searcher1", "correct horse battery staple", "Searcher One", "NORTH");
        registerUser("kariolsen", "correct horse battery staple", "Kari Olsen", "NORTH");
        String token = loginAndGetToken("searcher1", "correct horse battery staple");

        mockMvc.perform(get("/api/users/search").param("q", "kari").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].username").value("kariolsen"));

        mockMvc.perform(get("/api/users/search").param("q", "searcher1").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/search").param("q", "kari"))
            .andExpect(status().isUnauthorized());
    }

    private void registerUser(String username, String password, String fullName, String district) throws Exception {
        var request = String.format("""
            {
              "fullName": "%s",
              "username": "%s",
              "email": "%s@example.test",
              "password": "%s",
              "confirmPassword": "%s",
              "district": "%s"
            }
            """, fullName, username, username, password, password, district);

        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(request))
            .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        var request = String.format("""
            {
              "username": "%s",
              "password": "%s"
            }
            """, username, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(request))
            .andExpect(status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        return body.split("\"token\":\"")[1].split("\"")[0];
    }
}
