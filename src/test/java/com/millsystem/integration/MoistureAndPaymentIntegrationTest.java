package com.millsystem.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Moisture deductions and OTP-confirmed payments end to end: global table (super admin), mill override
 * (mill admin), purchase pricing, and a payment released only with the farmer's OTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MoistureAndPaymentIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockBean private SmsSender smsSender;

    private static Map<String, Object> band(Object from, Object to, Object deduction) {
        var m = new java.util.HashMap<String, Object>();
        m.put("fromPct", from); m.put("toPct", to); m.put("deductionPct", deduction);
        return m;
    }

    @Test
    void moistureDeductionPricingAndOtpConfirmedPayment() throws Exception {
        String superToken = login("superadmin", "Admin@123");
        JsonNode mill = call(post("/api/admin/mills"), superToken, Map.of("name", "Moisture Mill", "adminUsername", "moist_admin"), 201);
        String admin = login("moist_admin", mill.get("generatedPassword").asText());
        JsonNode op = call(post("/api/mill/operators"), admin, Map.of("username", "moist_op", "fullName", "Op"), 201);
        String operator = login("moist_op", op.get("generatedPassword").asText());

        try {
            // 1. Super admin sets the global table (the example from the requirement, gap closed at 19+).
            List<Map<String, Object>> global = List.of(band(0, 15, 0), band(15, 17, 5), band(17, 19, 20), band(19, null, 25));
            call(put("/api/admin/moisture-deductions"), superToken, global, 200);
            call(put("/api/admin/moisture-deductions"), superToken, List.of(band(0, 16, 0), band(15, null, 5)), 400); // overlap
            call(put("/api/admin/moisture-deductions"), admin, global, 403);                                          // mill admin: not global

            JsonNode table = call(get("/api/moisture-deductions"), operator, null, 200);
            assertThat(table.get("source").asText()).isEqualTo("GLOBAL");
            assertThat(table.get("ranges").size()).isEqualTo(4);

            // 2. A farmer with a verified mobile.
            long farmerId = registerFarmer(admin, "9876500077");

            // 3. Purchase at 16.2% moisture → 5% deduction: 100 q × 2000 = 200000 − 10000 = 190000.
            JsonNode purchase = call(post("/api/purchases"), operator, purchaseBody(farmerId, "16.2"), 201);
            assertThat(purchase.get("deductionPct").decimalValue()).isEqualByComparingTo("5");
            assertThat(purchase.get("grossAmount").decimalValue()).isEqualByComparingTo("200000");
            assertThat(purchase.get("deductionAmount").decimalValue()).isEqualByComparingTo("10000");
            assertThat(purchase.get("totalAmount").decimalValue()).isEqualByComparingTo("190000");
            call(post("/api/purchases"), operator, Map.of("farmerId", farmerId, "purchaseDate", "2026-11-01",
                    "weightQuintals", 1, "pricePerQuintal", 1), 400); // moisture required

            // 4. Mill admin overrides for their mill; operators cannot; the old purchase keeps its stored deduction.
            call(put("/api/moisture-deductions"), operator, List.of(band(16, null, 10)), 403);
            JsonNode mine = call(put("/api/moisture-deductions"), admin, List.of(band(16, null, 10)), 200);
            assertThat(mine.get("source").asText()).isEqualTo("MILL");
            JsonNode p2 = call(post("/api/purchases"), operator, purchaseBody(farmerId, "16.2"), 201);
            assertThat(p2.get("deductionPct").decimalValue()).isEqualByComparingTo("10");
            assertThat(p2.get("totalAmount").decimalValue()).isEqualByComparingTo("180000");
            JsonNode p3 = call(post("/api/purchases"), operator, purchaseBody(farmerId, "15.5"), 201); // no mill band → 0
            assertThat(p3.get("deductionPct").decimalValue()).isEqualByComparingTo("0");
            assertThat(call(get("/api/purchases/" + purchase.get("id").asLong()), operator, null, 200)
                    .get("deductionPct").decimalValue()).isEqualByComparingTo("5");
            // Reset → back to global.
            assertThat(call(delete("/api/moisture-deductions"), admin, null, 200).get("source").asText()).isEqualTo("GLOBAL");

            // Dev admin turns the moisture feature off for this mill: no moisture needed, nothing deducted,
            // settings API blocked. Turning it back on restores the rule.
            String dev = login("devadmin", "DevTest@123");
            long millId = mill.get("millId").asLong();
            call(put("/api/dev/mills/" + millId + "/features/moisture_deduction"), dev, Map.of("enabled", false), 200);
            JsonNode plain = call(post("/api/purchases"), operator, Map.of("farmerId", farmerId, "purchaseDate", "2026-11-01",
                    "weightQuintals", 100, "pricePerQuintal", 2000, "moisturePct", 25), 201);
            assertThat(plain.get("moisturePct").isNull()).isTrue();
            assertThat(plain.get("totalAmount").decimalValue()).isEqualByComparingTo("200000");
            call(post("/api/purchases"), operator, Map.of("farmerId", farmerId, "purchaseDate", "2026-11-01",
                    "weightQuintals", 1, "pricePerQuintal", 1), 201);
            call(get("/api/moisture-deductions"), operator, null, 403);
            call(delete("/api/dev/mills/" + millId + "/features/moisture_deduction"), dev, null, 204);
            call(post("/api/purchases"), operator, Map.of("farmerId", farmerId, "purchaseDate", "2026-11-01",
                    "weightQuintals", 1, "pricePerQuintal", 1), 400);

            // 5. Payment: OTP required, sent to the farmer, bound to the amount.
            long purchaseId = purchase.get("id").asLong();
            Map<String, Object> payment = Map.of("purchaseId", purchaseId, "amount", 50000, "paymentDate", "2026-11-01", "paymentMode", "UPI");
            assertThat(call(post("/api/payments"), operator, payment, 400).get("error").asText())
                    .isEqualTo("Farmer's OTP is required to release this payment");
            call(post("/api/payments/otp/send"), operator, Map.of("purchaseId", purchaseId, "amount", 190000.01), 400); // over balance

            reset(smsSender);
            JsonNode sent = call(post("/api/payments/otp/send"), operator, Map.of("purchaseId", purchaseId, "amount", 50000), 200);
            assertThat(sent.get("phone").asText()).isEqualTo("••••••0077");
            ArgumentCaptor<String> sms = ArgumentCaptor.forClass(String.class);
            verify(smsSender).send(eq("9876500077"), sms.capture());
            assertThat(sms.getValue()).contains("Rs.50000.00");
            String otp = sms.getValue().substring(0, 6);

            // An OTP for 50000 cannot release a different amount.
            Map<String, Object> tampered = Map.of("purchaseId", purchaseId, "amount", 60000, "paymentDate", "2026-11-01", "otp", otp);
            call(post("/api/payments"), operator, tampered, 400);

            var withOtp = new java.util.HashMap<String, Object>(payment);
            withOtp.put("otp", otp);
            JsonNode paid = call(post("/api/payments"), operator, withOtp, 201);
            assertThat(paid.get("otpVerifiedAt").isNull()).isFalse();
            assertThat(paid.has("otp")).isFalse();
            call(post("/api/payments"), operator, withOtp, 400); // OTP is single-use

            JsonNode after = call(get("/api/purchases/" + purchaseId), operator, null, 200);
            assertThat(after.get("paymentStatus").asText()).isEqualTo("PARTIAL");
            assertThat(after.get("amountDue").decimalValue()).isEqualByComparingTo("140000");
        } finally {
            call(put("/api/admin/moisture-deductions"), superToken, List.of(), 200); // shared test DB
        }
    }

    private long registerFarmer(String token, String phone) throws Exception {
        call(post("/api/farmers/otp/send"), token, Map.of("phone", phone), 200);
        ArgumentCaptor<String> sms = ArgumentCaptor.forClass(String.class);
        verify(smsSender, atLeastOnce()).send(eq(phone), sms.capture());
        String code = sms.getValue().substring(0, 6);
        String verification = call(post("/api/farmers/otp/verify"), token, Map.of("phone", phone, "otp", code), 200)
                .get("verificationToken").asText();
        return call(post("/api/farmers"), token, Map.of("name", "Moisture Farmer", "phone", phone,
                "phoneVerificationToken", verification), 201).get("id").asLong();
    }

    private static Map<String, Object> purchaseBody(long farmerId, String moisture) {
        return Map.of("farmerId", farmerId, "purchaseDate", "2026-11-01", "weightQuintals", 100,
                "pricePerQuintal", 2000, "moisturePct", new java.math.BigDecimal(moisture));
    }

    private String login(String username, String password) throws Exception {
        return call(post("/api/auth/login"), null, Map.of("username", username, "password", password), 200).get("token").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder req, String token, Object body, int expectedStatus) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        String response = mvc.perform(req).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return response.isEmpty() ? null : json.readTree(response);
    }
}
