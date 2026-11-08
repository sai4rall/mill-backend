package com.millsystem.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Self-service password change and session invalidation, end to end. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChangePasswordIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    void changingPasswordSignsOutOtherSessionsAndKeepsThisOne() throws Exception {
        String superToken = login("superadmin", "Admin@123");
        JsonNode mill = call(post("/api/admin/mills"), superToken, Map.of("name", "Password Mill", "adminUsername", "pw_admin", "location", "Nalgonda", "contactPhone", "9876543210", "adminFullName", "Mill Admin"), 201);
        String original = mill.get("generatedPassword").asText();
        String tabA = login("pw_admin", original);
        Thread.sleep(1100); // JWT iat has second precision: make the "other session" clearly older than the change
        String tabB = login("pw_admin", original);

        // Validation and current-password checks (400, never 401, so the client is not logged out)
        call(post("/api/auth/change-password"), tabA, Map.of("currentPassword", "wrong", "newPassword", "NewPass456"), 400);
        assertThat(call(post("/api/auth/change-password"), tabA, Map.of("currentPassword", original, "newPassword", "short1"), 400)
                .get("fields").has("newPassword")).isTrue();
        call(post("/api/auth/change-password"), tabA, Map.of("currentPassword", original, "newPassword", original), 400);
        call(post("/api/auth/change-password"), null, Map.of("currentPassword", original, "newPassword", "NewPass456"), 401);

        Thread.sleep(1100);
        JsonNode changed = call(post("/api/auth/change-password"), tabB, Map.of("currentPassword", original, "newPassword", "NewPass456"), 200);
        String fresh = changed.get("token").asText();
        assertThat(changed.get("role").asText()).isEqualTo("MILL_ADMIN");

        // The session that changed it continues with the fresh token; older tokens are rejected.
        call(get("/api/farmers"), fresh, null, 200);
        JsonNode rejected = call(get("/api/farmers"), tabA, null, 401);
        assertThat(rejected.get("error").asText()).isEqualTo("Your password was changed. Please sign in again.");
        call(get("/api/farmers"), tabB, null, 401);

        // Old password no longer works; new one does.
        call(post("/api/auth/login"), null, Map.of("username", "pw_admin", "password", original), 401);
        login("pw_admin", "NewPass456");

        // An admin reset also signs the user out.
        Thread.sleep(1100);
        call(post("/api/admin/mills/" + mill.get("millId").asLong() + "/reset-password"), superToken, null, 200);
        call(get("/api/farmers"), fresh, null, 401);
    }

    @Test
    void everyRoleCanChangeTheirPassword() throws Exception {
        String dev = login("devadmin", "DevTest@123");
        JsonNode created = call(post("/api/dev/super-admins"), dev, Map.of("username", "pw_super"), 201);
        String superToken = login("pw_super", created.get("generatedPassword").asText());
        Thread.sleep(1100);
        String fresh = call(post("/api/auth/change-password"), superToken,
                Map.of("currentPassword", created.get("generatedPassword").asText(), "newPassword", "Sup3rSecret"), 200).get("token").asText();
        call(get("/api/admin/mills"), fresh, null, 200);
        login("pw_super", "Sup3rSecret");
    }

    private String login(String username, String password) throws Exception {
        return call(post("/api/auth/login"), null, Map.of("username", username, "password", password), 200).get("token").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder req, String token, Object body, int expected) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        String res = mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return res.isEmpty() ? null : json.readTree(res);
    }
}
