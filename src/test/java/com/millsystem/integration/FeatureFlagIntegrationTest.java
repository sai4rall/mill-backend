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

import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feature flags end to end: seeded defaults, per-mill override seen by that mill only, API enforcement,
 * shared lookups, super admin bypass, and global defaults.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeatureFlagIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    void flagsControlMenuAndApiAccessPerMill() throws Exception {
        String superToken = login("superadmin", "Admin@123");
        long millA = createMill(superToken, "Flag Mill A", "flag_admin_a");
        long millB = createMill(superToken, "Flag Mill B", "flag_admin_b");
        String adminA = login("flag_admin_a", generatedPasswords.get(millA));
        String adminB = login("flag_admin_b", generatedPasswords.get(millB));
        String devToken = login("devadmin", "DevTest@123");

        // Seeded on startup: every module shown and enabled.
        JsonNode flags = call(get("/api/features"), adminA, null, 200);
        assertThat(flags.size()).isEqualTo(com.millsystem.feature.FeatureKey.values().length);
        assertThat(flags).allSatisfy(f -> {
            assertThat(f.get("showInMenu").asBoolean()).isTrue();
            assertThat(f.get("enabled").asBoolean()).isTrue();
        });

        // Disable Farmers (kept in menu) and hide Truck Tracking for mill A only.
        call(put("/api/dev/mills/" + millA + "/features/farmers"), devToken, Map.of("enabled", false), 200);
        call(put("/api/dev/mills/" + millA + "/features/truck_tracking"), devToken, Map.of("showInMenu", false), 200);

        JsonNode farmersA = flag(call(get("/api/features"), adminA, null, 200), "farmers");
        assertThat(farmersA.get("showInMenu").asBoolean()).isTrue();
        assertThat(farmersA.get("enabled").asBoolean()).isFalse();
        assertThat(flag(call(get("/api/features"), adminA, null, 200), "truck_tracking").get("showInMenu").asBoolean()).isFalse();
        assertThat(flag(call(get("/api/features"), adminB, null, 200), "farmers").get("enabled").asBoolean()).isTrue();

        // Mill A can no longer use the Farmers module...
        JsonNode blocked = call(post("/api/farmers/otp/send"), adminA, Map.of("phone", "9876543210"), 403);
        assertThat(blocked.get("error").asText()).isEqualTo("The Farmers module is not enabled for your mill");
        assertThat(blocked.get("code").asText()).isEqualTo("FEATURE_DISABLED");
        // ...but the farmer list stays readable for the Purchases screen, and mill B is unaffected.
        call(get("/api/farmers"), adminA, null, 200);
        call(get("/api/farmers"), adminB, null, 200);
        // Super admin is never blocked.
        mvc.perform(get("/api/farmers/999").header("Authorization", "Bearer " + superToken).header("X-Mill-Id", millA))
                .andExpect(status().isNotFound());

        // Admin view shows global, override and effective values.
        JsonNode adminView = flag(call(get("/api/dev/mills/" + millA + "/features"), devToken, null, 200), "farmers");
        assertThat(adminView.get("globalEnabled").asBoolean()).isTrue();
        assertThat(adminView.get("overrideEnabled").asBoolean()).isFalse();
        assertThat(adminView.get("overrideShowInMenu").isNull()).isTrue();
        assertThat(adminView.get("enabled").asBoolean()).isFalse();

        // Removing the override restores access.
        mvc.perform(delete("/api/dev/mills/" + millA + "/features/farmers").header("Authorization", "Bearer " + devToken))
                .andExpect(status().isNoContent());
        call(get("/api/farmers/999"), adminA, null, 404);

        // Global default applies to every mill without an override; restore afterwards (shared test DB).
        try {
            call(put("/api/dev/features/stock_report"), devToken, Map.of("enabled", false), 200);
            call(get("/api/stock/statement"), adminB, null, 403);
            call(get("/api/stock/summary"), adminB, null, 200); // still used by the Dashboard
        } finally {
            call(put("/api/dev/features/stock_report"), devToken, Map.of("enabled", true), 200);
        }

        // Only the dev admin can manage flags (not mill users, not the super admin); unknown features are 404.
        for (String token : new String[]{adminA, superToken}) {
            mvc.perform(put("/api/dev/features/farmers").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                    .andExpect(status().isForbidden());
        }
        call(put("/api/dev/features/nope"), devToken, Map.of("enabled", false), 404);
    }

    private final Map<Long, String> generatedPasswords = new java.util.HashMap<>();

    private long createMill(String superToken, String name, String admin) throws Exception {
        JsonNode mill = call(post("/api/admin/mills"), superToken, Map.of("name", name, "adminUsername", admin), 201);
        generatedPasswords.put(mill.get("millId").asLong(), mill.get("generatedPassword").asText());
        return mill.get("millId").asLong();
    }

    private static JsonNode flag(JsonNode flags, String key) {
        return StreamSupport.stream(flags.spliterator(), false)
                .filter(f -> key.equals(f.get("key").asText())).findFirst().orElseThrow();
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
