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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Each super admin is a separate company: they see and manage only the mills they created, and nothing is shared. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyIsolationIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    void superAdminsOnlySeeAndManageTheirOwnCompanysMills() throws Exception {
        String dev = login("devadmin", "DevTest@123");
        JsonNode supA = call(post("/api/dev/super-admins"), dev, null, Map.of("username", "company_a"), 201);
        JsonNode supB = call(post("/api/dev/super-admins"), dev, null, Map.of("username", "company_b"), 201);
        String a = login("company_a", supA.get("generatedPassword").asText());
        String b = login("company_b", supB.get("generatedPassword").asText());

        long millA = call(post("/api/admin/mills"), a, null, Map.of("name", "A Rice Mill", "adminUsername", "a_mill_admin", "location", "Nalgonda", "contactPhone", "9876543210", "adminFullName", "Mill Admin"), 201).get("millId").asLong();
        long millB = call(post("/api/admin/mills"), b, null, Map.of("name", "B Rice Mill", "adminUsername", "b_mill_admin", "location", "Nalgonda", "contactPhone", "9876543210", "adminFullName", "Mill Admin"), 201).get("millId").asLong();

        // Lists show only the company's own mills.
        assertThat(call(get("/api/admin/mills"), a, null, null, 200).findValuesAsText("name")).containsExactly("A Rice Mill");
        assertThat(call(get("/api/admin/mills"), b, null, null, 200).findValuesAsText("name")).containsExactly("B Rice Mill");

        // Their own mill's data is readable; another company's mill does not exist for them.
        call(get("/api/stock/summary"), a, millA, null, 200);
        for (String path : List.of("/api/stock/summary", "/api/farmers", "/api/purchases", "/api/payments", "/api/orders",
                "/api/customers", "/api/mill-batches", "/api/shipments", "/api/moisture-deductions", "/api/stock/statement", "/api/features")) {
            int code = mvc.perform(get(path).header("Authorization", "Bearer " + a).header("X-Mill-Id", millB)).andReturn().getResponse().getStatus();
            assertThat(code).as(path).isEqualTo(404);
        }
        call(put("/api/admin/mills/" + millB), a, null, Map.of("name", "Taken", "adminUsername", "b_mill_admin", "location", "Nalgonda", "contactPhone", "9876543210", "adminFullName", "Mill Admin"), 404);
        call(patch("/api/admin/mills/" + millB + "/status"), a, null, null, 404);
        call(post("/api/admin/mills/" + millB + "/reset-password"), a, null, null, 404);
        assertThat(call(get("/api/admin/mills"), b, null, null, 200).findValuesAsText("name")).containsExactly("B Rice Mill");

        // Moisture defaults belong to one company only.
        call(put("/api/admin/moisture-deductions"), a, null, List.of(Map.of("fromPct", 15, "deductionPct", 5)), 200);
        assertThat(call(get("/api/admin/moisture-deductions"), b, null, null, 200)).isEmpty();
        assertThat(call(get("/api/moisture-deductions"), a, millA, null, 200).get("source").asText()).isEqualTo("GLOBAL");
        assertThat(call(get("/api/moisture-deductions"), b, millB, null, 200).get("source").asText()).isEqualTo("NONE");

        // The dev admin sees every mill with its company and can move a mill between companies.
        JsonNode devMills = call(get("/api/dev/mills"), dev, null, null, 200);
        assertThat(devMills.findParents("millName").stream().filter(m -> m.get("millId").asLong() == millB).findFirst().orElseThrow()
                .get("ownerUsername").asText()).isEqualTo("company_b");
        call(put("/api/dev/mills/" + millB + "/owner"), dev, null, Map.of("ownerId", supA.get("id").asLong()), 200);
        call(put("/api/dev/mills/" + millB + "/owner"), dev, null, Map.of("ownerId", 999999), 404);
        assertThat(call(get("/api/admin/mills"), a, null, null, 200).findValuesAsText("name")).containsExactlyInAnyOrder("A Rice Mill", "B Rice Mill");
        assertThat(call(get("/api/admin/mills"), b, null, null, 200)).isEmpty();
        call(get("/api/stock/summary"), b, millB, null, 404);

        // A company that still owns mills cannot be deleted; one without mills can.
        call(delete("/api/dev/super-admins/" + supA.get("id").asLong()), dev, null, null, 409);
        call(delete("/api/dev/super-admins/" + supB.get("id").asLong()), dev, null, null, 204);
    }

    @Test
    void creatingAMillValidatesEveryMandatoryField() throws Exception {
        String sup = login("superadmin", "Admin@123");
        JsonNode empty = call(post("/api/admin/mills"), sup, null, Map.of(), 400);
        assertThat(empty.get("fields").fieldNames()).toIterable()
                .contains("name", "location", "contactPhone", "adminUsername", "adminFullName");

        JsonNode bad = call(post("/api/admin/mills"), sup, null, Map.of("name", "V Mill", "location", "Guntur", "contactPhone", "12345",
                "contactEmail", "not-an-email", "adminUsername", "a b", "adminFullName", "R2"), 400);
        assertThat(bad.get("fields").get("contactPhone").asText()).isEqualTo("Enter a valid 10-digit mobile number");
        assertThat(bad.get("fields").fieldNames()).toIterable().contains("contactEmail", "adminUsername", "adminFullName");
        call(post("/api/admin/mills"), sup, null, Map.of("name", "V Mill", "location", "Guntur", "contactPhone", "98765432101",
                "adminUsername", "v_admin", "adminFullName", "Venkat Rao"), 400); // 11 digits

        // +91 and spaces are accepted and stored as the 10-digit number; email is optional.
        call(post("/api/admin/mills"), sup, null, Map.of("name", "V Mill", "location", "Guntur", "contactPhone", "+91 98765 43210",
                "adminUsername", "v_admin", "adminFullName", "Venkat Rao"), 201);
        JsonNode created = call(get("/api/admin/mills"), sup, null, null, 200).findParents("name").stream()
                .filter(m -> m.get("name").asText().equals("V Mill")).findFirst().orElseThrow();
        assertThat(created.get("contactPhone").asText()).isEqualTo("9876543210");
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
