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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full farmer registration through the real HTTP API, security, services and database (H2 in MySQL mode):
 * super admin creates a mill → mill admin logs in → OTP sent and verified → farmer registered → photo uploaded.
 * Only the SMS gateway is mocked, to capture the OTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FarmerRegistrationIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockBean private SmsSender smsSender;

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'};

    @Test
    void millAdminRegistersFarmerWithVerifiedMobileAndPhoto() throws Exception {
        // 1. Super admin creates a mill; its admin logs in with the generated password.
        String superToken = login("superadmin", "Admin@123");
        JsonNode mill = call(post("/api/admin/mills"), superToken,
                Map.of("name", "Integration Mill", "adminUsername", "int_admin"), 201);
        String token = login("int_admin", mill.get("generatedPassword").asText());

        // 2. Field validation rejects a bad request with per-field messages, before any OTP is involved.
        JsonNode invalid = call(post("/api/farmers"), token,
                Map.of("name", "R2", "phone", "9876543210", "bankAccount", "12AB", "ifscCode", "XXXX"), 400);
        assertThat(invalid.get("fields").fieldNames()).toIterable()
                .contains("name", "bankAccount", "ifscCode", "bankDetailsComplete");

        // 3. Registering without OTP verification is refused.
        JsonNode unverified = call(post("/api/farmers"), token, farmerBody("9876543210", null), 400);
        assertThat(unverified.get("error").asText()).contains("not verified");

        // 4. Send OTP (number given in +91 format) and read it from the mocked SMS gateway.
        call(post("/api/farmers/otp/send"), token, Map.of("phone", "+91 98765 43210"), 200);
        ArgumentCaptor<String> sms = ArgumentCaptor.forClass(String.class);
        verify(smsSender).send(eq("9876543210"), sms.capture());
        String otp = sms.getValue().substring(0, 6);

        // 5. Wrong OTP fails, correct OTP returns a verification token.
        call(post("/api/farmers/otp/verify"), token, Map.of("phone", "9876543210", "otp", "000000".equals(otp) ? "111111" : "000000"), 400);
        String verificationToken = call(post("/api/farmers/otp/verify"), token,
                Map.of("phone", "9876543210", "otp", otp), 200).get("verificationToken").asText();

        // 6. Register the farmer; values are normalised and the phone is marked verified.
        JsonNode farmer = call(post("/api/farmers"), token, farmerBody("+91 98765 43210", verificationToken), 201);
        long id = farmer.get("id").asLong();
        assertThat(farmer.get("name").asText()).isEqualTo("Ramesh Kumar");
        assertThat(farmer.get("phone").asText()).isEqualTo("9876543210");
        assertThat(farmer.get("phoneVerified").asBoolean()).isTrue();
        assertThat(farmer.get("bankAccount").asText()).isEqualTo("123456789012");
        assertThat(farmer.get("ifscCode").asText()).isEqualTo("SBIN0001234");
        assertThat(farmer.has("phoneVerificationToken")).isFalse();

        // 7. The token is single-use.
        call(post("/api/farmers"), token, farmerBody("9876543210", verificationToken), 400);

        // 8. Upload a photo and read it back.
        mvc.perform(multipart("/api/farmers/{id}/photo", id)
                        .file(new MockMultipartFile("file", "p.jpg", "image/jpeg", JPEG))
                        .with(r -> { r.setMethod("PUT"); return r; })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUpdatedAt").isNotEmpty());
        mvc.perform(get("/api/farmers/{id}/photo", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(JPEG));

        // 9. The farmer appears in the mill's list with verified phone and photo.
        mvc.perform(get("/api/farmers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].phoneVerified").value(true))
                .andExpect(jsonPath("$[0].photoUpdatedAt").isNotEmpty());
    }

    private Map<String, Object> farmerBody(String phone, String token) {
        var body = new java.util.HashMap<String, Object>(Map.of(
                "name", "  Ramesh   Kumar ", "phone", phone, "address", "Nalgonda",
                "bankAccount", "1234 5678 9012", "bankName", "State Bank of India", "ifscCode", "sbin0001234"));
        if (token != null) body.put("phoneVerificationToken", token);
        return body;
    }

    private String login(String username, String password) throws Exception {
        return call(post("/api/auth/login"), null, Map.of("username", username, "password", password), 200)
                .get("token").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder req, String token, Object body, int expectedStatus) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        String response = mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? null : json.readTree(response);
    }
}
