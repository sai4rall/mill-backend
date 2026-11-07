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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rejected lorries: recorded per mill, validated, never touching stock, and read-only for super admins. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RejectedLorryIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    void millUsersRecordRejectedLorriesForTheirOwnMill() throws Exception {
        String sup = login("superadmin", "Admin@123");
        JsonNode mill = call(post("/api/admin/mills"), sup, null, Map.of("name", "Reject Mill", "adminUsername", "reject_admin"), 201);
        long millId = mill.get("millId").asLong();
        String admin = login("reject_admin", mill.get("generatedPassword").asText());
        JsonNode other = call(post("/api/admin/mills"), sup, null, Map.of("name", "Other Reject Mill", "adminUsername", "reject_other"), 201);
        String otherAdmin = login("reject_other", other.get("generatedPassword").asText());
        JsonNode stockBefore = call(get("/api/stock/summary"), admin, null, null, 200);

        // Record: truck number normalised, recorder kept, no farmer needed.
        JsonNode r = call(post("/api/rejected-lorries"), admin, null, body("ap 07 cd 1234", "HIGH_MOISTURE", null), 201);
        assertThat(r.get("truckNumber").asText()).isEqualTo("AP07CD1234");
        assertThat(r.get("recordedBy").asText()).isEqualTo("reject_admin");
        assertThat(r.get("reason").asText()).isEqualTo("HIGH_MOISTURE");
        assertThat(r.get("farmerId").isNull()).isTrue();

        // Validation.
        Map<String, Object> future = body("AP07CD1", "POOR_QUALITY", null);
        future.put("rejectionDate", LocalDate.now().plusDays(2).toString());
        assertThat(call(post("/api/rejected-lorries"), admin, null, future, 400).get("fields").has("rejectionDate")).isTrue();
        assertThat(call(post("/api/rejected-lorries"), admin, null, body("", "POOR_QUALITY", null), 400).get("fields").has("truckNumber")).isTrue();
        assertThat(call(post("/api/rejected-lorries"), admin, null, body("AP07CD1", "OTHER", null), 400).get("fields").has("reasonExplained")).isTrue();
        call(post("/api/rejected-lorries"), admin, null, body("AP07CD1", "OTHER", "Driver left before weighing"), 201);
        call(post("/api/rejected-lorries"), admin, null, body("AP07CD1", "BAD", null), 400);
        Map<String, Object> unknownFarmer = body("AP07CD1", "POOR_QUALITY", null);
        unknownFarmer.put("farmerId", 999999);
        call(post("/api/rejected-lorries"), admin, null, unknownFarmer, 404);

        // Listed newest first for this mill only; stock unchanged.
        assertThat(call(get("/api/rejected-lorries"), admin, null, null, 200)).hasSize(2);
        assertThat(call(get("/api/rejected-lorries"), otherAdmin, null, null, 200)).isEmpty();
        assertThat(call(get("/api/stock/summary"), admin, null, null, 200)).isEqualTo(stockBefore);

        // Another mill cannot change it; the owning mill can correct and delete it.
        long id = r.get("id").asLong();
        call(put("/api/rejected-lorries/" + id), otherAdmin, null, body("X1", "POOR_QUALITY", null), 404);
        call(delete("/api/rejected-lorries/" + id), otherAdmin, null, null, 404);
        assertThat(call(put("/api/rejected-lorries/" + id), admin, null, body("AP07CD1234", "POOR_QUALITY", "Broken grains"), 200)
                .get("reason").asText()).isEqualTo("POOR_QUALITY");

        // Super admin (owning company) can view but not record; the dev admin cannot see mill data at all.
        assertThat(call(get("/api/rejected-lorries"), sup, millId, null, 200)).hasSize(2);
        call(post("/api/rejected-lorries"), sup, millId, body("AP1", "POOR_QUALITY", null), 403);
        call(get("/api/rejected-lorries"), login("devadmin", "DevTest@123"), millId, null, 403);

        call(delete("/api/rejected-lorries/" + id), admin, null, null, 204);
        assertThat(call(get("/api/rejected-lorries"), admin, null, null, 200)).hasSize(1);
    }

    private static Map<String, Object> body(String truck, String reason, String notes) {
        Map<String, Object> b = new HashMap<>(Map.of("rejectionDate", LocalDate.now().toString(), "truckNumber", truck, "reason", reason,
                "approxWeightQuintals", 120.5, "moisturePct", 24));
        if (notes != null) b.put("notes", notes);
        return b;
    }

    private String login(String username, String password) throws Exception {
        return call(post("/api/auth/login"), null, null, Map.of("username", username, "password", password), 200).get("token").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder req, String token, Long millId, Object body, int expected) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (millId != null) req.header("X-Mill-Id", millId);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        String response = mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? null : json.readTree(response);
    }
}
