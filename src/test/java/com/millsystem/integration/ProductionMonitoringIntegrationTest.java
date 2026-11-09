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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Production monitoring: shifts, by-products, recovery %, losses, downtime, and shift / day / mill comparisons. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductionMonitoringIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private FarmerRepository farmers;
    @Autowired private MillRepository mills;

    @Test
    void tracksProductionRecoveryLossesDowntimeAndShifts() throws Exception {
        String today = LocalDate.now().toString();
        String sup = login("superadmin", "Admin@123");
        JsonNode mill = call(post("/api/admin/mills"), sup, null, Map.of("name", "Prod Mill", "adminUsername", "prod_admin",
                "location", "Nalgonda", "contactPhone", "9876543210", "adminFullName", "Prod Admin"), 201);
        long millId = mill.get("millId").asLong();
        String admin = login("prod_admin", mill.get("generatedPassword").asText());

        // 1,000 Qtl of paddy in stock.
        Farmer f = new Farmer();
        f.setMill(mills.findById(millId).orElseThrow()); f.setName("Prod Farmer"); f.setPhone("9000000001");
        long farmerId = farmers.save(f).getId();
        call(post("/api/purchases"), admin, null, Map.of("farmerId", farmerId, "purchaseDate", today, "weightQuintals", 1000,
                "pricePerQuintal", 2000, "moisturePct", 12), 201);

        // Morning: 100 in, 65 rice + 5 broken + 8 bran + 20 husk → 2 unaccounted, 65% recovery, 3 short of the 68 expected.
        long morning = call(post("/api/mill-batches"), admin, null, batch(today, 100, 68, "MORNING"), 201).get("id").asLong();
        Map<String, Object> done = new HashMap<>(Map.of("actualRiceQuintals", 65, "brokenRiceQuintals", 5, "branQuintals", 8, "huskQuintals", 20));
        JsonNode m = call(put("/api/mill-batches/" + morning + "/complete"), admin, null, done, 200);
        assertThat(m.get("shift").asText()).isEqualTo("MORNING");
        assertThat(m.get("unaccountedQuintals").decimalValue()).isEqualByComparingTo("2");
        assertThat(m.get("conversionRate").decimalValue()).isEqualByComparingTo("65");

        // Night: 200 in, 120 rice → 60%. Rice + by-products may not exceed the input.
        long night = call(post("/api/mill-batches"), admin, null, batch(today, 200, null, "NIGHT"), 201).get("id").asLong();
        call(put("/api/mill-batches/" + night + "/complete"), admin, null, Map.of("actualRiceQuintals", 120, "huskQuintals", 90), 400);
        call(put("/api/mill-batches/" + night + "/complete"), admin, null, Map.of("actualRiceQuintals", 120, "huskQuintals", 50), 200);
        call(post("/api/mill-batches"), admin, null, batch(today, 50, null, "AFTERNOON"), 201); // still running
        call(post("/api/mill-batches"), admin, null, batch(today, 10, null, "EVENING"), 400);   // unknown shift

        // Downtime: a 45-minute husker breakdown and a power cut that runs past midnight (23:30 → 01:00 = 90 min).
        JsonNode d1 = call(post("/api/downtime"), admin, null, stop(today, "MORNING", "Husker", "09:15", "10:00", "BREAKDOWN", null), 201);
        assertThat(d1.get("durationMinutes").asInt()).isEqualTo(45);
        assertThat(call(post("/api/downtime"), admin, null, stop(today, "NIGHT", "Main line", "23:30", "01:00", "POWER_CUT", null), 201)
                .get("durationMinutes").asInt()).isEqualTo(90);
        call(post("/api/downtime"), admin, null, stop(today, "NIGHT", "Polisher", "02:00", "02:00", "BREAKDOWN", null), 400); // zero length
        call(post("/api/downtime"), admin, null, stop(today, "NIGHT", "Polisher", "02:00", "03:00", "OTHER", null), 400);     // Other needs notes
        call(post("/api/downtime"), admin, null, stop(LocalDate.now().plusDays(1).toString(), "NIGHT", "Polisher", "02:00", "03:00", "BREAKDOWN", null), 400);
        assertThat(call(get("/api/downtime"), admin, null, null, 200)).hasSize(2);

        // Report: totals, per shift, per day, downtime groups.
        JsonNode r = call(get("/api/production").param("from", today).param("to", today), admin, null, null, 200);
        JsonNode t = r.get("totals");
        assertThat(t.get("batches").asInt()).isEqualTo(2);
        assertThat(r.get("batchesInProgress").asInt()).isEqualTo(1);
        assertThat(t.get("paddyInQuintals").decimalValue()).isEqualByComparingTo("300");
        assertThat(t.get("riceQuintals").decimalValue()).isEqualByComparingTo("185");
        assertThat(t.get("recoveryPct").decimalValue()).isEqualByComparingTo("61.67");
        assertThat(t.get("unaccountedQuintals").decimalValue()).isEqualByComparingTo("32"); // 2 + (200 - 120 - 50)
        assertThat(t.get("shortfallQuintals").decimalValue()).isEqualByComparingTo("3");
        assertThat(t.get("downtimeMinutes").asInt()).isEqualTo(135);
        JsonNode shifts = r.get("shifts");
        assertThat(shifts.findValuesAsText("key")).containsExactly("MORNING", "AFTERNOON", "NIGHT");
        assertThat(shifts.get(0).get("recoveryPct").decimalValue()).isEqualByComparingTo("65");
        assertThat(shifts.get(2).get("recoveryPct").decimalValue()).isEqualByComparingTo("60");
        assertThat(shifts.get(2).get("downtimeMinutes").asInt()).isEqualTo(90);
        assertThat(shifts.get(1).get("batches").asInt()).isZero(); // afternoon batch not finished yet
        assertThat(r.get("days")).hasSize(1);
        assertThat(r.get("downtimeByReason").get(0).get("key").asText()).isEqualTo("POWER_CUT");
        assertThat(r.get("downtimeByMachine").findValuesAsText("key")).containsExactly("Main line", "Husker");
        call(get("/api/production").param("from", today).param("to", LocalDate.now().minusDays(1).toString()), admin, null, null, 400);

        // The owning super admin sees the mill's report and the comparison across their mills; the dev admin sees neither.
        assertThat(call(get("/api/production"), sup, millId, null, 200).get("totals").get("batches").asInt()).isEqualTo(2);
        JsonNode cmp = call(get("/api/admin/production").param("from", today).param("to", today), sup, null, null, 200);
        JsonNode mine = cmp.findParents("millName").stream().filter(x -> x.get("millId").asLong() == millId).findFirst().orElseThrow();
        assertThat(mine.get("totals").get("riceQuintals").decimalValue()).isEqualByComparingTo("185");
        call(post("/api/downtime"), sup, millId, stop(today, "NIGHT", "Polisher", "02:00", "03:00", "BREAKDOWN", null), 403);
        call(get("/api/production"), login("devadmin", "DevTest@123"), millId, null, 403);
    }

    private static Map<String, Object> batch(String date, int paddy, Integer expected, String shift) {
        Map<String, Object> b = new HashMap<>(Map.of("batchDate", date, "paddyInputQuintals", paddy, "shift", shift));
        if (expected != null) b.put("expectedRiceQuintals", expected);
        return b;
    }

    private static Map<String, Object> stop(String date, String shift, String machine, String start, String end, String reason, String notes) {
        Map<String, Object> s = new HashMap<>(Map.of("downtimeDate", date, "shift", shift, "machine", machine, "startTime", start, "endTime", end, "reason", reason));
        if (notes != null) s.put("notes", notes);
        return s;
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
