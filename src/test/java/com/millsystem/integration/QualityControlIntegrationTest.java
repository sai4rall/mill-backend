package com.millsystem.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.millsystem.entity.Farmer;
import com.millsystem.repository.FarmerRepository;
import com.millsystem.repository.MillRepository;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quality control: standards, lab tests judged against them, batch history, certificates, isolation and roles. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QualityControlIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private FarmerRepository farmers;
    @Autowired private MillRepository mills;

    @Test
    @SuppressWarnings("unchecked")
    void recordsJudgesCertifiesAndIsolatesLabTests() throws Exception {
        String today = LocalDate.now().toString();
        String sup = login("superadmin", "Admin@123");
        JsonNode mill = call(post("/api/admin/mills"), sup, null, Map.of("name", "QC Mill", "adminUsername", "qc_admin", "location", "Nalgonda",
                "contactPhone", "9876543210", "adminFullName", "QC Admin"), 201);
        long millId = mill.get("millId").asLong();
        String admin = login("qc_admin", mill.get("generatedPassword").asText());
        JsonNode other = call(post("/api/admin/mills"), sup, null, Map.of("name", "QC Other", "adminUsername", "qc_other", "location", "Nalgonda",
                "contactPhone", "9876543210", "adminFullName", "QC Other"), 201);
        String otherAdmin = login("qc_other", other.get("generatedPassword").asText());
        JsonNode op = call(post("/api/mill/operators"), admin, null, Map.of("username", "qc_lab", "fullName", "Lab Tech"), 201);
        String operator = login("qc_lab", op.get("generatedPassword").asText());

        // Standards belong to the super admin's company. Defaults until saved; mill users can read but not change them.
        JsonNode stds = call(get("/api/quality/standards"), admin, null, null, 200);
        assertThat(stds).hasSize(2);
        assertThat(stds.get(1).get("product").asText()).isEqualTo("RICE");
        assertThat(stds.get(1).get("configured").asBoolean()).isFalse();
        assertThat(stds.get(1).get("grades")).hasSize(5);
        call(put("/api/quality/standards/RICE"), admin, null, riceStandard("20"), 404); // no mill-level endpoint any more
        call(get("/api/admin/quality-standards"), admin, null, null, 403);
        call(put("/api/admin/quality-standards/RICE"), admin, null, riceStandard("20"), 403);

        // Super admin validation: all five grades, sane numbers, a better grade never looser than a worse one.
        call(put("/api/admin/quality-standards/RICE"), sup, null, Map.of("grades", List.of(grade("C", 14, 20))), 400);
        call(put("/api/admin/quality-standards/RICE"), sup, null, riceStandard("120"), 400);
        Map<String, Object> inverted = riceStandard("20");
        ((List<Map<String, Object>>) inverted.get("grades")).set(0, grade("A_PLUS", 13, 30));
        assertThat(call(put("/api/admin/quality-standards/RICE"), sup, null, inverted, 400).get("error").asText())
                .contains("Broken").contains("A+").contains("looser");
        JsonNode saved = call(put("/api/admin/quality-standards/RICE"), sup, null, riceStandard("20"), 200);
        assertThat(saved.get("configured").asBoolean()).isTrue();
        assertThat(saved.get("updatedBy").asText()).isEqualTo("superadmin");
        assertThat(call(get("/api/quality/standards"), operator, null, null, 200).get(1).get("grades").get(4).get("maxBrokenPct").decimalValue())
                .isEqualByComparingTo("20");

        // A batch to test the rice of.
        Farmer f = new Farmer();
        f.setMill(mills.findById(millId).orElseThrow()); f.setName("QC Farmer"); f.setPhone("9000000002");
        long farmerId = farmers.save(f).getId();
        call(post("/api/purchases"), admin, null, Map.of("farmerId", farmerId, "purchaseDate", today, "weightQuintals", 500,
                "pricePerQuintal", 2000, "moisturePct", 13), 201).get("id").asLong();
        long batchId = call(post("/api/mill-batches"), admin, null, Map.of("batchDate", today, "paddyInputQuintals", 100, "shift", "MORNING"), 201)
                .get("id").asLong();

        // Paddy sample identified by its lot: passes the default paddy limits.
        JsonNode paddy = call(post("/api/quality/tests"), operator, null, Map.of("testDate", today, "product", "PADDY", "lotReference", "Stack 4",
                "moisturePct", 16.5, "foreignMatterPct", 1), 201);
        assertThat(paddy.get("result").asText()).isEqualTo("PASS");
        assertThat(paddy.get("gradeLabel").asText()).isEqualTo("C"); // moisture 16.5 is within C (17) but not B (16)
        assertThat(paddy.get("lotReference").asText()).isEqualTo("Stack 4");
        assertThat(paddy.has("purchaseId")).isFalse();
        assertThat(paddy.get("recordedBy").asText()).isEqualTo("qc_lab");

        // Rice samples from the batch: one fails on broken (20% limit), one passes.
        JsonNode bad = call(post("/api/quality/tests"), operator, null, rice(today, batchId, 13, 22.5), 201);
        assertThat(bad.get("result").asText()).isEqualTo("FAIL");
        assertThat(bad.get("failureReasons").get(0).asText()).isEqualTo("Broken 22.50% is above the limit 20.00%");
        assertThat(bad.get("grade").isNull()).isTrue();
        assertThat(bad.get("standard").get("grades").get(4).get("maxBrokenPct").decimalValue()).isEqualByComparingTo("20");
        JsonNode good = call(post("/api/quality/tests"), operator, null, rice(today, batchId, 13, 18), 201);
        assertThat(good.get("result").asText()).isEqualTo("PASS");
        assertThat(good.get("grade").asText()).isEqualTo("B"); // broken 18 is within B (18) but not B+ (15)
        assertThat(good.get("gradeLabel").asText()).isEqualTo("B");
        assertThat(good.get("sampleCode").asText()).isEqualTo("LAB-" + good.get("id").asLong());

        // Validation.
        Map<String, Object> noBroken = rice(today, batchId, 13, 0);
        noBroken.remove("brokenPct");
        assertThat(call(post("/api/quality/tests"), admin, null, noBroken, 400).get("fields").has("brokenGiven")).isTrue();
        assertThat(call(post("/api/quality/tests"), admin, null, rice(LocalDate.now().plusDays(3).toString(), batchId, 13, 10), 400)
                .get("fields").has("testDate")).isTrue();
        assertThat(call(post("/api/quality/tests"), admin, null, rice(today, batchId, 101, 10), 400).get("fields").has("moisturePct")).isTrue();
        call(post("/api/quality/tests"), admin, null, rice(LocalDate.now().minusDays(5).toString(), batchId, 13, 10), 400); // before the batch
        call(post("/api/quality/tests"), otherAdmin, null, rice(today, batchId, 13, 10), 404); // another mill's batch

        // Filters and batch history.
        assertThat(call(get("/api/quality/tests?result=FAIL"), admin, null, null, 200)).hasSize(1);
        assertThat(call(get("/api/quality/tests?batchId=" + batchId), admin, null, null, 200)).hasSize(2);
        JsonNode history = call(get("/api/quality/batches"), admin, null, null, 200);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).get("testCount").asInt()).isEqualTo(2);
        assertThat(history.get(0).get("failCount").asInt()).isEqualTo(1);
        assertThat(history.get(0).get("latestResult").asText()).isEqualTo("PASS");
        assertThat(history.get(0).get("latestGrade").asText()).isEqualTo("B");
        assertThat(history.get(0).get("avgBrokenPct").decimalValue()).isEqualByComparingTo("20.25");

        // Re-testing a failed sample after correcting it re-judges it.
        long badId = bad.get("id").asLong();
        assertThat(call(put("/api/quality/tests/" + badId), admin, null, rice(today, batchId, 13, 19), 200).get("result").asText()).isEqualTo("PASS");
        call(put("/api/quality/tests/" + badId), admin, null, rice(today, batchId, 13, 22.5), 200);

        // Certificates: only for passed samples, numbered per mill per year, idempotent, and they lock the test.
        call(post("/api/quality/tests/" + badId + "/certificate"), admin, null, null, 409);
        long goodId = good.get("id").asLong();
        JsonNode cert = call(post("/api/quality/tests/" + goodId + "/certificate"), operator, null, null, 200);
        String number = cert.get("certificateNumber").asText();
        assertThat(number).isEqualTo("QC-" + LocalDate.now().getYear() + "-0001");
        assertThat(cert.get("certificateIssuedBy").asText()).isEqualTo("qc_lab");
        assertThat(call(post("/api/quality/tests/" + goodId + "/certificate"), admin, null, null, 200).get("certificateNumber").asText()).isEqualTo(number);
        assertThat(call(post("/api/quality/tests/" + paddy.get("id").asLong() + "/certificate"), admin, null, null, 200)
                .get("certificateNumber").asText()).endsWith("-0002");
        call(put("/api/quality/tests/" + goodId), admin, null, rice(today, batchId, 12, 10), 409);
        call(delete("/api/quality/tests/" + goodId), admin, null, null, 409);

        // Isolation and roles.
        assertThat(call(get("/api/quality/tests"), otherAdmin, null, null, 200)).isEmpty();
        call(get("/api/quality/tests/" + goodId), otherAdmin, null, null, 404);
        call(delete("/api/quality/tests/" + badId), otherAdmin, null, null, 404);
        assertThat(call(get("/api/quality/tests"), sup, millId, null, 200)).hasSize(3);
        call(post("/api/quality/tests"), sup, millId, rice(today, batchId, 13, 10), 403);
        call(get("/api/quality/tests"), login("devadmin", "DevTest@123"), millId, null, 403);
        // The standard is company-wide: the super admin's other mill uses it too.
        assertThat(call(get("/api/quality/standards"), otherAdmin, null, null, 200).get(1).get("configured").asBoolean()).isTrue();

        call(delete("/api/quality/tests/" + badId), admin, null, null, 204);
        assertThat(call(get("/api/quality/tests"), admin, null, null, 200)).hasSize(2);
    }

    /** Rice grades A+ → C with broken limits 5/10/15/18/{cBroken} and moisture 13/13.5/13.5/14/14. */
    private static Map<String, Object> riceStandard(String cBroken) {
        return new HashMap<>(Map.of("grades", new java.util.ArrayList<>(List.of(grade("A_PLUS", 13, 5), grade("A", 13.5, 10), grade("B_PLUS", 13.5, 15),
                grade("B", 14, 18), grade("C", 14, Double.parseDouble(cBroken))))));
    }

    private static Map<String, Object> grade(String grade, double moisture, double broken) {
        return new HashMap<>(Map.of("grade", grade, "maxMoisturePct", moisture, "maxBrokenPct", broken, "maxForeignMatterPct", 0.5));
    }

    private static Map<String, Object> rice(String date, long batchId, double moisture, double broken) {
        return new HashMap<>(Map.of("testDate", date, "product", "RICE", "batchId", batchId, "moisturePct", moisture, "brokenPct", broken,
                "testedBy", "  Lab   Chemist ", "variety", "Sona Masoori"));
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
