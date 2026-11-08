package com.millsystem.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.millsystem.config.AppTime;
import com.millsystem.sms.SmsSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regression tests for the issues in qa/bug-report.csv (BUG-001 … BUG-022, backend part). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QaRegressionIntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockBean private SmsSender smsSender;

    private record MillCtx(long millId, String admin, String operator, long farmerId, String adminUser, String adminPassword) {}

    private String today() { return AppTime.today().toString(); }

    /** A fresh mill with an admin, an operator and one OTP-verified farmer. */
    private MillCtx newMill() throws Exception {
        int n = SEQ.incrementAndGet();
        String superToken = login("superadmin", "Admin@123");
        JsonNode mill = call(post("/api/admin/mills"), superToken, Map.of("name", "QA Mill " + n, "adminUsername", "qa_admin_" + n, "location", "Nalgonda", "contactPhone", "9876543210", "adminFullName", "Mill Admin"), 201);
        String admin = login("qa_admin_" + n, mill.get("generatedPassword").asText());
        JsonNode op = call(post("/api/mill/operators"), admin, Map.of("username", "qa_op_" + n, "fullName", "Op"), 201);
        String operator = login("qa_op_" + n, op.get("generatedPassword").asText());
        String phone = String.format("90000%05d", n);
        call(post("/api/farmers/otp/send"), admin, Map.of("phone", phone), 200);
        ArgumentCaptor<String> sms = ArgumentCaptor.forClass(String.class);
        verify(smsSender, atLeastOnce()).send(eq(phone), sms.capture());
        String vt = call(post("/api/farmers/otp/verify"), admin, Map.of("phone", phone, "otp", sms.getValue().substring(0, 6)), 200)
                .get("verificationToken").asText();
        long farmerId = call(post("/api/farmers"), admin, Map.of("name", "QA Farmer", "phone", phone, "phoneVerificationToken", vt), 201).get("id").asLong();
        return new MillCtx(mill.get("millId").asLong(), admin, operator, farmerId, "qa_admin_" + n, mill.get("generatedPassword").asText());
    }

    private long purchase(MillCtx m, Object weight) throws Exception {
        return call(post("/api/purchases"), m.admin(), Map.of("farmerId", m.farmerId(), "purchaseDate", today(),
                "weightQuintals", weight, "pricePerQuintal", 2000, "moisturePct", 12), 201).get("id").asLong();
    }

    private JsonNode stock(MillCtx m) throws Exception { return call(get("/api/stock/summary"), m.admin(), null, 200); }

    @Test
    void bug001_paymentStatusCannotBeSetByClients() throws Exception {
        MillCtx m = newMill();
        long p = purchase(m, 10);
        call(put("/api/purchases/" + p), m.operator(), Map.of("paymentStatus", "PAID", "notes", "trying"), 200);
        assertThat(call(get("/api/payments"), m.operator(), null, 200)).isEmpty(); // all-payments list (Money screen)
        JsonNode after = call(get("/api/purchases/" + p), m.admin(), null, 200);
        assertThat(after.get("paymentStatus").asText()).isEqualTo("PENDING");
        assertThat(after.get("notes").asText()).isEqualTo("trying");
    }

    @Test
    void bug004_005_011_batchesConsumePaddyAndAreBounded() throws Exception {
        MillCtx m = newMill();
        purchase(m, 10);
        JsonNode tooMuch = call(post("/api/mill-batches"), m.admin(), Map.of("batchDate", today(), "paddyInputQuintals", 11, "purchaseEntries", new Object[0]), 400);
        assertThat(tooMuch.get("error").asText()).contains("exceeds paddy on hand");
        long batch = call(post("/api/mill-batches"), m.admin(), Map.of("batchDate", today(), "paddyInputQuintals", 6, "purchaseEntries", new Object[0]), 201).get("id").asLong();
        JsonNode s = stock(m);
        assertThat(s.get("paddyOnHand").decimalValue()).isEqualByComparingTo("4");
        assertThat(s.get("paddyInMill").decimalValue()).isEqualByComparingTo("6");

        call(put("/api/mill-batches/" + batch + "/complete"), m.admin(), Map.of("actualRiceQuintals", 7), 400); // more rice than paddy
        call(put("/api/mill-batches/" + batch + "/complete"), m.admin(), Map.of("actualRiceQuintals", 4.123), 400); // 3 decimals
        call(put("/api/mill-batches/" + batch + "/complete"), m.admin(), Map.of("actualRiceQuintals", 4), 200);
        s = stock(m);
        assertThat(s.get("paddyOnHand").decimalValue()).isEqualByComparingTo("4");
        assertThat(s.get("paddyInMill").decimalValue()).isEqualByComparingTo("0");
        assertThat(s.get("riceOnHand").decimalValue()).isEqualByComparingTo("4");
    }

    @Test
    void bug003_shipmentsBoundedByOrderAndRiceOnHand() throws Exception {
        MillCtx m = newMill();
        purchase(m, 10);
        long batch = call(post("/api/mill-batches"), m.admin(), Map.of("batchDate", today(), "paddyInputQuintals", 10, "purchaseEntries", new Object[0]), 201).get("id").asLong();
        call(put("/api/mill-batches/" + batch + "/complete"), m.admin(), Map.of("actualRiceQuintals", 4), 200);
        long customer = call(post("/api/customers"), m.admin(), Map.of("name", "Buyer"), 201).get("id").asLong();
        long small = call(post("/api/orders"), m.admin(), Map.of("customerId", customer, "orderDate", today(), "quantityQuintals", 3), 201).get("id").asLong();
        long big = call(post("/api/orders"), m.admin(), Map.of("customerId", customer, "orderDate", today(), "quantityQuintals", 10), 201).get("id").asLong();

        assertThat(call(post("/api/shipments"), m.admin(), ship(small, 3.5), 400).get("error").asText()).contains("left on the order");
        call(post("/api/shipments"), m.admin(), ship(small, 3), 201);
        assertThat(call(post("/api/shipments"), m.admin(), ship(small, 0.5), 400).get("error").asText()).contains("fulfilled");
        assertThat(call(post("/api/shipments"), m.admin(), ship(big, 2), 400).get("error").asText()).contains("rice on hand");
        call(post("/api/shipments"), m.admin(), ship(big, 1), 201);
        assertThat(stock(m).get("riceOnHand").decimalValue()).isEqualByComparingTo("0");

        // BUG-020: the Shipments page lists every shipment of the mill, with the customer.
        JsonNode all = call(get("/api/shipments"), m.operator(), null, 200);
        assertThat(all).hasSize(2);
        assertThat(all.get(0).get("customerName").asText()).isEqualTo("Buyer");
        assertThat(call(get("/api/shipments?orderId=" + small), m.admin(), null, 200)).hasSize(1);
    }

    private Map<String, Object> ship(long orderId, Object qty) {
        return Map.of("orderId", orderId, "quintalsExported", qty, "shipmentDate", today());
    }

    @Test
    void bug009_012_013_014_018_badInputIsA4xxNotA500() throws Exception {
        MillCtx m = newMill();
        assertThat(raw(post("/api/purchases"), m.admin(), "{\"farmerId\":", 400)).contains("Malformed");
        assertThat(raw(post("/api/purchases"), m.admin(), "{\"farmerId\":" + m.farmerId() + ",\"purchaseDate\":\"" + today()
                + "\",\"weightQuintals\":\"abc\",\"pricePerQuintal\":1,\"moisturePct\":1}", 400)).contains("weightQuintals");
        Map<String, Object> big = new HashMap<>(Map.of("farmerId", m.farmerId(), "purchaseDate", today(), "pricePerQuintal", 1, "moisturePct", 1));
        big.put("weightQuintals", 1_000_000_000_000L);
        call(post("/api/purchases"), m.admin(), big, 400);
        big.put("weightQuintals", 1.2345);
        call(post("/api/purchases"), m.admin(), big, 400);
        long p = purchase(m, 10);
        assertThat(call(post("/api/payments/otp/send"), m.admin(), Map.of("purchaseId", p, "amount", 10.555), 400).toString()).contains("decimal");
        assertThat(raw(post("/api/payments"), m.admin(), "{\"purchaseId\":" + p + ",\"amount\":1,\"paymentDate\":\"" + today()
                + "\",\"paymentMode\":\"BITCOIN\",\"otp\":\"000000\"}", 400)).contains("Allowed");

        call(post("/api/customers"), m.admin(), Map.of("name", "x".repeat(300)), 400);
        call(post("/api/customers"), m.admin(), Map.of("name", "Ok", "email", "not-an-email"), 400);
        call(post("/api/customers"), m.admin(), Map.of("name", "Ok", "phone", "abc"), 400);
        call(post("/api/customers"), m.admin(), Map.of("name", "Ok", "phone", "", "email", ""), 201); // blank optional fields are fine

        call(post("/api/mill/operators"), m.admin(), Map.of("username", "a b<script>"), 400);
        JsonNode created = call(post("/api/mill/operators"), m.admin(), Map.of("username", "  Mixed.Case_1 "), 201);
        assertThat(created.get("username").asText()).isEqualTo("mixed.case_1");
        call(post("/api/mill/operators"), m.admin(), Map.of("username", "MIXED.CASE_1"), 409);
    }

    @Test
    void bug010_farmerWithPurchasesCannotBeDeleted() throws Exception {
        MillCtx m = newMill();
        purchase(m, 1);
        assertThat(call(delete("/api/farmers/" + m.farmerId()), m.admin(), null, 409).get("error").asText()).contains("cannot be deleted");
    }

    @Test
    void bug017_datesUseTheMillTimeZoneAndHaveALowerBound() throws Exception {
        MillCtx m = newMill();
        Map<String, Object> body = new HashMap<>(Map.of("farmerId", m.farmerId(), "weightQuintals", 1, "pricePerQuintal", 1, "moisturePct", 1));
        body.put("purchaseDate", AppTime.today().plusDays(1).toString());
        call(post("/api/purchases"), m.admin(), body, 400);
        body.put("purchaseDate", "1990-01-01");
        call(post("/api/purchases"), m.admin(), body, 400);
        body.put("purchaseDate", today());
        call(post("/api/purchases"), m.admin(), body, 201);
    }

    @Test
    void bug006_022_badOrSignedOutTokensGet401() throws Exception {
        MillCtx m = newMill();
        assertThat(call(get("/api/farmers"), null, null, 401).get("error").asText()).isEqualTo("Please sign in to continue");
        assertThat(call(get("/api/farmers"), "abc.def.ghi", null, 401).get("error").asText()).contains("Invalid session");
        call(get("/api/admin/mills"), m.admin(), null, 403); // signed in but not allowed stays 403

        String token = login(m.adminUser(), m.adminPassword()); // a second session for the same user
        call(get("/api/farmers"), token, null, 200);
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        assertThat(call(get("/api/farmers"), token, null, 401).get("error").asText()).contains("signed out");
        call(get("/api/farmers"), m.admin(), null, 200); // other sessions unaffected

        // A stale or invalid token left in a browser must not block signing in again.
        mvc.perform(post("/api/auth/login").header("Authorization", "Bearer abc.def.ghi").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", m.adminUser(), "password", m.adminPassword()))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", m.adminUser(), "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void bug008_repeatedFailedLoginsAreThrottled() throws Exception {
        MillCtx m = newMill();
        String user = "qa_op_" + SEQ.get();
        for (int i = 0; i < 5; i++) call(post("/api/auth/login"), null, Map.of("username", user, "password", "wrong" + i), 401);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username", user, "password", "wrong"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists("Retry-After"));
        assertThat(m.operator()).isNotBlank(); // existing sessions keep working
        call(get("/api/farmers"), m.operator(), null, 200);
    }

    @Test
    void bug007_gpsNeedsADeviceKeyAndSaneCoordinates() throws Exception {
        MillCtx m = newMill();
        Map<String, Object> ping = Map.of("truckNumber", "AP07CD1234", "latitude", 17.4, "longitude", 78.5);
        call(post("/api/gps/ping"), null, ping, 401);
        assertThat(call(get("/api/mill/gps-key"), m.admin(), null, 200).get("configured").asBoolean()).isFalse();
        call(post("/api/mill/gps-key/rotate"), m.operator(), null, 403); // admin only
        String key = call(post("/api/mill/gps-key/rotate"), m.admin(), null, 200).get("deviceKey").asText();
        assertThat(call(get("/api/mill/gps-key"), m.admin(), null, 200).get("configured").asBoolean()).isTrue();

        device(key, ping, 201);
        device(key, Map.of("truckNumber", "AP07CD1234", "latitude", 999, "longitude", -999), 400);
        device(key, Map.of("truckNumber", "AP07CD1234", "latitude", 17.4, "longitude", 78.5, "millId", m.millId() + 999), 400);
        device("gps_wrong", ping, 401);
        assertThat(call(get("/api/trucks/numbers"), m.admin(), null, 200).toString()).contains("AP07CD1234");

        String newKey = call(post("/api/mill/gps-key/rotate"), m.admin(), null, 200).get("deviceKey").asText();
        device(key, ping, 401);    // the old key stops working
        device(newKey, ping, 201);
    }

    private void device(String key, Map<String, Object> body, int expected) throws Exception {
        mvc.perform(post("/api/gps/ping").header("X-Device-Key", key).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().is(expected));
    }

    @Test
    void bug019_021_rolesForTrucksAndSuperAdminWrites() throws Exception {
        MillCtx m = newMill();
        call(get("/api/trucks/numbers"), m.operator(), null, 403);
        call(get("/api/stock/summary"), m.operator(), null, 200); // stock report is for all mill users

        String superToken = login("superadmin", "Admin@123");
        mvc.perform(get("/api/stock/summary").header("Authorization", "Bearer " + superToken).header("X-Mill-Id", m.millId()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/customers").header("Authorization", "Bearer " + superToken).header("X-Mill-Id", m.millId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"By super\"}"))
                .andExpect(status().isForbidden());
    }

    // ---- helpers ----

    private String login(String username, String password) throws Exception {
        return call(post("/api/auth/login"), null, Map.of("username", username, "password", password), 200).get("token").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder req, String token, Object body, int expected) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        String res = mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return res.isEmpty() ? json.createObjectNode() : json.readTree(res);
    }

    private String raw(MockHttpServletRequestBuilder req, String token, String body, int expected) throws Exception {
        req.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
