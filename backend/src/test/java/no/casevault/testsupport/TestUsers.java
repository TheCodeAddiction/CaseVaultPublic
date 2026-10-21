package no.casevault.testsupport;

import no.casevault.model.District;
import no.casevault.model.Role;
import no.casevault.model.User;
import no.casevault.repository.UserRepository;
import no.casevault.service.PasswordHasher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared helpers for tests that need an authenticated user of a given role.
 * Self-registration (POST /api/auth/register) always creates an OFFICER, so
 * SUPERVISOR/ADMIN test users are inserted directly via the repository.
 */
public final class TestUsers {

    public static final String DEFAULT_PASSWORD = "correct horse battery staple";

    private TestUsers() {
    }

    public static String registerAndLoginOfficer(MockMvc mockMvc, String username, District district) throws Exception {
        var request = String.format("""
            {
              "fullName": "Test User",
              "username": "%s",
              "email": "%s@example.test",
              "password": "%s",
              "confirmPassword": "%s",
              "district": "%s"
            }
            """, username, username, DEFAULT_PASSWORD, DEFAULT_PASSWORD, district);

        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(request))
            .andExpect(status().isCreated());

        return login(mockMvc, username);
    }

    public static String createAndLoginUser(MockMvc mockMvc, UserRepository userRepository, PasswordHasher passwordHasher,
                                             String username, Role role, District district) throws Exception {
        userRepository.save(new User(username, username + "@example.test", "Test User", passwordHasher.hash(DEFAULT_PASSWORD), district, role));
        return login(mockMvc, username);
    }

    public static String login(MockMvc mockMvc, String username) throws Exception {
        var request = String.format("""
            {
              "username": "%s",
              "password": "%s"
            }
            """, username, DEFAULT_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(request))
            .andExpect(status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        return body.split("\"token\":\"")[1].split("\"")[0];
    }
}
