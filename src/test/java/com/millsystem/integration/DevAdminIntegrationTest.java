package com.millsystem.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Dev admin end to end: seeded login, super admin management, mill logos shown to the mill's users,
 * and role separation (dev admin has no mill data access; others cannot use dev endpoints).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DevAdminIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    void devAdminManagesSuperAdminsAndMillLogos() throws Exception {
        // Seeded dev admin (password from config) can log in and is identified by role.
        JsonNode devLogin = call(post("/api/auth/login"), null, Map.of("username", "devadmin", "password", "DevTest@123"), 200);
        assertThat(devLogin.get("role").asText()).isEqualTo("DEV_ADMIN");
        String dev = devLogin.get("token").asText();

        // 1. Dev admin creates a super admin, who can log in with the one-time password.
        JsonNode created = call(post("/api/dev/super-admins"), dev, Map.of("username", "ops_super", "fullName", "Ops Super"), 201);
        String superToken = login("ops_super", created.get("generatedPassword").asText());
        call(post("/api/dev/super-admins"), dev, Map.of("username", "ops_super"), 409);           // duplicate
        call(post("/api/dev/super-admins"), dev, Map.of("username", "OPS_SUPER"), 409);           // duplicate, other case
        call(post("/api/dev/super-admins"), dev, Map.of("username", "a b"), 400);                 // invalid
        JsonNode supers = call(get("/api/dev/super-admins"), dev, null, 200);
        assertThat(supers.findValuesAsText("username")).contains("superadmin", "ops_super");
        assertThat(supers.toString()).doesNotContain("generatedPassword\":\"");

        // 2. The new super admin creates two mills.
        JsonNode millA = call(post("/api/admin/mills"), superToken, Map.of("name", "Logo Mill A", "adminUsername", "logo_admin_a"), 201);
        JsonNode millB = call(post("/api/admin/mills"), superToken, Map.of("name", "Logo Mill B", "adminUsername", "logo_admin_b"), 201);
        long idA = millA.get("millId").asLong();
        String adminA = login("logo_admin_a", millA.get("generatedPassword").asText());
        String adminB = login("logo_admin_b", millB.get("generatedPassword").asText());

        // 3. Dev admin sees every mill and uploads a logo for mill A.
        assertThat(call(get("/api/dev/mills"), dev, null, 200).findValuesAsText("millName")).contains("Logo Mill A", "Logo Mill B");
        mvc.perform(upload(idA, new MockMultipartFile("file", "logo.svg", "image/svg+xml", "<svg/>".getBytes()), dev))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Logo must be a PNG, JPEG or WebP image"));
        mvc.perform(upload(idA, new MockMultipartFile("file", "logo.png", "image/png", PNG), dev))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUpdatedAt").isNotEmpty());

        // 4. Mill A's users get their mill name + logo for the header; mill B has no logo.
        JsonNode brandingA = call(get("/api/mill/branding"), adminA, null, 200);
        assertThat(brandingA.get("millName").asText()).isEqualTo("Logo Mill A");
        assertThat(brandingA.get("logoUpdatedAt").isNull()).isFalse();
        mvc.perform(get("/api/mill/logo").header("Authorization", "Bearer " + adminA))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(PNG));
        assertThat(call(get("/api/mill/branding"), adminB, null, 200).get("logoUpdatedAt").isNull()).isTrue();
        call(get("/api/mill/logo"), adminB, null, 404);

        // 5. Removing the logo.
        mvc.perform(delete("/api/dev/mills/{id}/logo", idA).header("Authorization", "Bearer " + dev)).andExpect(status().isNoContent());
        assertThat(call(get("/api/mill/branding"), adminA, null, 200).get("logoUpdatedAt").isNull()).isTrue();

        // 6. Role separation.
        call(get("/api/farmers"), dev, null, 403);                    // dev admin: no mill data
        call(get("/api/admin/mills"), dev, null, 403);                // dev admin: not a super admin
        call(get("/api/dev/super-admins"), superToken, null, 403);    // super admin: no dev endpoints
        call(get("/api/dev/mills"), adminA, null, 403);               // mill admin: no dev endpoints
        mvc.perform(get("/api/mill/branding").header("Authorization", "Bearer " + dev)).andExpect(status().isNoContent());

        // 7. Disabling the super admin blocks their login; reset gives a working new password.
        long superId = created.get("id").asLong();
        call(patch("/api/dev/super-admins/" + superId + "/status"), dev, null, 200);
        call(post("/api/auth/login"), null, Map.of("username", "ops_super", "password", created.get("generatedPassword").asText()), 403);
        call(patch("/api/dev/super-admins/" + superId + "/status"), dev, null, 200);
        String newPassword = call(post("/api/dev/super-admins/" + superId + "/reset-password"), dev, null, 200).get("generatedPassword").asText();
        login("ops_super", newPassword);
    }

    @Test
    void devAdminDeactivatesAndDeletesMillsAndSuperAdminsAndResetsAnyPassword() throws Exception {
        String dev = login("devadmin", "DevTest@123");
        long devId = call(get("/api/dev/users"), dev, null, 200).findParents("username").stream()
                .filter(u -> u.get("username").asText().equals("devadmin")).findFirst().orElseThrow().get("id").asLong();

        JsonNode sup = call(post("/api/dev/super-admins"), dev, Map.of("username", "gone_super"), 201);
        String supToken = login("gone_super", sup.get("generatedPassword").asText());
        JsonNode mill = call(post("/api/admin/mills"), supToken, Map.of("name", "Doomed Mill", "adminUsername", "doomed_admin"), 201);
        long millId = mill.get("millId").asLong();
        String admin = login("doomed_admin", mill.get("generatedPassword").asText());
        JsonNode op = call(post("/api/mill/operators"), admin, Map.of("username", "doomed_op"), 201);
        call(post("/api/customers"), admin, Map.of("name", "Buyer"), 201);

        // Every account is listed with its mill; no password data.
        JsonNode all = call(get("/api/dev/users"), dev, null, 200);
        JsonNode opRow = all.findParents("username").stream().filter(u -> u.get("username").asText().equals("doomed_op")).findFirst().orElseThrow();
        assertThat(opRow.get("role").asText()).isEqualTo("MILL_OPERATOR");
        assertThat(opRow.get("millName").asText()).isEqualTo("Doomed Mill");
        assertThat(all.toString()).doesNotContain("password\"");

        // Reset any password: the old session ends, the new password works; not for the dev admin's own account.
        String opToken = login("doomed_op", op.get("generatedPassword").asText());
        Thread.sleep(1100); // token iat has 1 s resolution
        String newOp = call(post("/api/dev/users/" + opRow.get("id").asLong() + "/reset-password"), dev, null, 200).get("generatedPassword").asText();
        call(get("/api/customers"), opToken, null, 401);
        login("doomed_op", newOp);
        call(post("/api/dev/users/" + devId + "/reset-password"), dev, null, 400);
        call(post("/api/dev/users/999999/reset-password"), dev, null, 404);
        call(post("/api/dev/users/" + opRow.get("id").asLong() + "/reset-password"), supToken, null, 403);

        // Deactivate the mill: its users are signed out and cannot sign in; reactivate restores access.
        call(patch("/api/dev/mills/" + millId + "/status"), dev, Map.of("status", "INACTIVE"), 200);
        call(get("/api/customers"), admin, null, 401);
        call(post("/api/auth/login"), null, Map.of("username", "doomed_admin", "password", mill.get("generatedPassword").asText()), 403);
        call(patch("/api/dev/mills/" + millId + "/status"), dev, Map.of("status", "SOMETHING"), 400);
        call(patch("/api/dev/mills/" + millId + "/status"), dev, Map.of("status", "ACTIVE"), 200);
        admin = login("doomed_admin", mill.get("generatedPassword").asText());

        // Delete the mill: needs the exact name; removes its users and data.
        call(delete("/api/dev/mills/" + millId), dev, null, 400);
        call(delete("/api/dev/mills/" + millId).param("confirm", "doomed mill"), dev, null, 400);
        call(delete("/api/dev/mills/" + millId).param("confirm", "Doomed Mill"), supToken, null, 403);
        JsonNode removed = call(delete("/api/dev/mills/" + millId).param("confirm", "Doomed Mill"), dev, null, 200);
        assertThat(removed.get("users").asInt()).isEqualTo(2);
        assertThat(removed.get("customers").asInt()).isEqualTo(1);
        call(get("/api/customers"), admin, null, 401);
        call(post("/api/auth/login"), null, Map.of("username", "doomed_op", "password", newOp), 401);
        assertThat(call(get("/api/dev/mills"), dev, null, 200).findValuesAsText("millName")).doesNotContain("Doomed Mill");
        call(delete("/api/dev/mills/" + millId).param("confirm", "Doomed Mill"), dev, null, 404);

        // Delete a super admin: their session stops working; only super admins can be deleted this way.
        long supId = sup.get("id").asLong();
        call(delete("/api/dev/super-admins/" + devId), dev, null, 404);
        call(delete("/api/dev/super-admins/" + supId), dev, null, 204);
        call(get("/api/admin/mills"), supToken, null, 401);
        assertThat(call(get("/api/dev/super-admins"), dev, null, 200).findValuesAsText("username")).doesNotContain("gone_super");
    }

    private MockMultipartHttpServletRequestBuilder upload(long millId, MockMultipartFile file, String token) {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/dev/mills/{id}/logo", millId).file(file);
        req.with(r -> { r.setMethod("PUT"); return r; });
        req.header("Authorization", "Bearer " + token);
        return req;
    }

    private String login(String username, String password) throws Exception {
        return call(post("/api/auth/login"), null, Map.of("username", username, "password", password), 200).get("token").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder req, String token, Object body, int expectedStatus) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        String response = mvc.perform(req).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? null : json.readTree(response);
    }
}
