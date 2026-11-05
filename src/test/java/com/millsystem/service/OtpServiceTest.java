package com.millsystem.service;

import com.millsystem.entity.Mill;
import com.millsystem.entity.PhoneOtp;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.PhoneOtpRepository;
import com.millsystem.sms.SmsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final long MILL_ID = 7L;
    private static final String PHONE = "9876543210";

    @Mock private PhoneOtpRepository otpRepository;
    @Mock private MillRepository millRepository;
    @Mock private SmsSender smsSender;
    // BCrypt with minimum cost keeps tests fast while exercising real hashing.
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private OtpService service;
    private Mill mill;

    @BeforeEach
    void setUp() {
        service = new OtpService(otpRepository, millRepository, encoder, smsSender);
        ReflectionTestUtils.setField(service, "expiryMinutes", 5);
        ReflectionTestUtils.setField(service, "resendCooldownSeconds", 30);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "maxSendsPerHour", 5);
        ReflectionTestUtils.setField(service, "tokenValidityMinutes", 15);
        mill = new Mill();
        mill.setId(MILL_ID);
        mill.setName("Test Mill");
    }

    private PhoneOtp otp(String code, LocalDateTime createdAt, LocalDateTime expiresAt) {
        PhoneOtp o = new PhoneOtp();
        o.setMill(mill);
        o.setPhone(PHONE);
        o.setOtpHash(encoder.encode(code));
        o.setExpiresAt(expiresAt);
        ReflectionTestUtils.setField(o, "createdAt", createdAt);
        return o;
    }

    @Nested
    class NormalizePhone {
        @ParameterizedTest
        @CsvSource({
                "9876543210, 9876543210",
                "6000000000, 6000000000",
                "+919876543210, 9876543210",
                "+91 98765 43210, 9876543210",
                "919876543210, 9876543210",
                "09876543210, 9876543210",
                "98765-43210, 9876543210",
                "(98765) 43210, 9876543210"
        })
        void acceptsIndianMobileFormats(String raw, String expected) {
            assertThat(OtpService.normalizePhone(raw)).isEqualTo(expected);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"12345", "5876543210", "0123456789", "98765432101", "987654321", "98765abcde", "+1 9876543210"})
        void rejectsInvalidNumbers(String raw) {
            assertThatThrownBy(() -> OtpService.normalizePhone(raw))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Enter a valid 10-digit mobile number");
        }
    }

    @Nested
    class Send {
        @Test
        void storesHashedOtpAndSendsSmsToNormalisedNumber() {
            when(millRepository.findById(MILL_ID)).thenReturn(Optional.of(mill));
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, OtpService.PURPOSE_PHONE_VERIFY)).thenReturn(Optional.empty());

            int expiresIn = service.send(MILL_ID, "+91 98765 43210");

            assertThat(expiresIn).isEqualTo(300);
            ArgumentCaptor<PhoneOtp> saved = ArgumentCaptor.forClass(PhoneOtp.class);
            verify(otpRepository).save(saved.capture());
            ArgumentCaptor<String> sms = ArgumentCaptor.forClass(String.class);
            verify(smsSender).send(eq(PHONE), sms.capture());

            String code = sms.getValue().substring(0, 6);
            assertThat(code).matches("\\d{6}");
            assertThat(sms.getValue()).contains("Test Mill");
            assertThat(saved.getValue().getPhone()).isEqualTo(PHONE);
            assertThat(saved.getValue().getPurpose()).isEqualTo(OtpService.PURPOSE_PHONE_VERIFY);
            assertThat(saved.getValue().getOtpHash()).isNotEqualTo(code);
            assertThat(encoder.matches(code, saved.getValue().getOtpHash())).isTrue();
            assertThat(saved.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(4));
        }

        @Test
        void rejectsInvalidNumberWithoutSending() {
            assertThatThrownBy(() -> service.send(MILL_ID, "12345")).isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(smsSender, otpRepository);
        }

        @Test
        void enforcesResendCooldown() {
            when(millRepository.findById(MILL_ID)).thenReturn(Optional.of(mill));
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, OtpService.PURPOSE_PHONE_VERIFY))
                    .thenReturn(Optional.of(otp("111111", LocalDateTime.now().minusSeconds(10), LocalDateTime.now().plusMinutes(5))));

            assertThatThrownBy(() -> service.send(MILL_ID, PHONE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageStartingWith("Please wait");
            verifyNoInteractions(smsSender);
        }

        @Test
        void enforcesHourlyLimit() {
            when(millRepository.findById(MILL_ID)).thenReturn(Optional.of(mill));
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, OtpService.PURPOSE_PHONE_VERIFY)).thenReturn(Optional.empty());
            when(otpRepository.countByMillIdAndPhoneAndCreatedAtAfter(eq(MILL_ID), eq(PHONE), any())).thenReturn(5L);

            assertThatThrownBy(() -> service.send(MILL_ID, PHONE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Too many OTP requests");
            verifyNoInteractions(smsSender);
        }
    }

    @Nested
    class Verify {
        @Test
        void correctOtpReturnsTokenAndMarksVerified() {
            PhoneOtp o = otp("123456", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, OtpService.PURPOSE_PHONE_VERIFY)).thenReturn(Optional.of(o));

            String token = service.verify(MILL_ID, PHONE, "123456");

            assertThat(token).isNotBlank().isEqualTo(o.getVerificationToken());
            assertThat(o.getVerifiedAt()).isNotNull();
        }

        @Test
        void wrongOtpCountsAttempt() {
            PhoneOtp o = otp("123456", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, OtpService.PURPOSE_PHONE_VERIFY)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> service.verify(MILL_ID, PHONE, "000000"))
                    .hasMessage("Incorrect OTP. 4 attempt(s) left.");
            assertThat(o.getAttempts()).isEqualTo(1);
            assertThat(o.getVerificationToken()).isNull();
            verify(otpRepository).save(o);
        }

        @Test
        void blocksAfterMaxAttemptsEvenWithCorrectOtp() {
            PhoneOtp o = otp("123456", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
            o.setAttempts(5);
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, OtpService.PURPOSE_PHONE_VERIFY)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> service.verify(MILL_ID, PHONE, "123456"))
                    .hasMessageContaining("Too many incorrect attempts");
        }

        @Test
        void expiredOtpIsRejected() {
            PhoneOtp o = otp("123456", LocalDateTime.now().minusMinutes(6), LocalDateTime.now().minusMinutes(1));
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, OtpService.PURPOSE_PHONE_VERIFY)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> service.verify(MILL_ID, PHONE, "123456"))
                    .hasMessageContaining("OTP expired or not requested");
        }

        @Test
        void alreadyVerifiedOtpCannotBeReused() {
            PhoneOtp o = otp("123456", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
            o.setVerifiedAt(LocalDateTime.now());
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, OtpService.PURPOSE_PHONE_VERIFY)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> service.verify(MILL_ID, PHONE, "123456"))
                    .hasMessageContaining("OTP expired or not requested");
        }

        @Test
        void noOtpRequestedIsRejected() {
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, OtpService.PURPOSE_PHONE_VERIFY)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.verify(MILL_ID, PHONE, "123456"))
                    .hasMessageContaining("OTP expired or not requested");
        }
    }

    @Nested
    class PurposeBoundCodes {
        private static final String PURPOSE = "PAYMENT:12:5000.00";

        @Test
        void sendCodeStoresPurposeAndUsesCustomMessage() {
            when(millRepository.findById(MILL_ID)).thenReturn(Optional.of(mill));
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, PURPOSE)).thenReturn(Optional.empty());

            service.sendCode(MILL_ID, PHONE, PURPOSE, (code, m) -> "PAY " + code + " @ " + m.getName());

            ArgumentCaptor<PhoneOtp> saved = ArgumentCaptor.forClass(PhoneOtp.class);
            verify(otpRepository).save(saved.capture());
            assertThat(saved.getValue().getPurpose()).isEqualTo(PURPOSE);
            ArgumentCaptor<String> sms = ArgumentCaptor.forClass(String.class);
            verify(smsSender).send(eq(PHONE), sms.capture());
            assertThat(sms.getValue()).matches("PAY \\d{6} @ Test Mill");
        }

        @Test
        void cooldownIsPerPurpose() {
            when(millRepository.findById(MILL_ID)).thenReturn(Optional.of(mill));
            // A phone-verification SMS 5s ago does not block a payment OTP...
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, PURPOSE)).thenReturn(Optional.empty());
            service.sendCode(MILL_ID, PHONE, PURPOSE, (code, m) -> code);
            verify(smsSender).send(eq(PHONE), anyString());
            // ...but a payment OTP 5s ago blocks another one for the same purpose.
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, PURPOSE))
                    .thenReturn(Optional.of(otp("111111", LocalDateTime.now().minusSeconds(5), LocalDateTime.now().plusMinutes(5))));
            assertThatThrownBy(() -> service.sendCode(MILL_ID, PHONE, PURPOSE, (code, m) -> code)).hasMessageStartingWith("Please wait");
        }

        @Test
        void confirmCodeUsesTheCodeUpImmediately() {
            PhoneOtp o = otp("123456", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
            o.setPurpose(PURPOSE);
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, PURPOSE)).thenReturn(Optional.of(o));

            service.confirmCode(MILL_ID, PHONE, PURPOSE, "123456");

            assertThat(o.getVerifiedAt()).isNotNull();
            assertThat(o.getConsumedAt()).isNotNull();
            assertThat(o.getVerificationToken()).isNull();
            // A used code cannot be confirmed again.
            assertThatThrownBy(() -> service.confirmCode(MILL_ID, PHONE, PURPOSE, "123456"))
                    .hasMessageContaining("OTP expired or not requested");
        }

        @Test
        void confirmCodeRejectsWrongCodeAndMissingCode() {
            PhoneOtp o = otp("123456", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, PURPOSE)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> service.confirmCode(MILL_ID, PHONE, PURPOSE, "999999")).hasMessage("Incorrect OTP. 4 attempt(s) left.");
            assertThatThrownBy(() -> service.confirmCode(MILL_ID, PHONE, PURPOSE, null)).hasMessage("Incorrect OTP. 3 attempt(s) left.");
            assertThat(o.getConsumedAt()).isNull();
        }

        @Test
        void codeForAnotherPurposeIsNotFound() {
            when(otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(MILL_ID, PHONE, "PAYMENT:12:9999.00"))
                    .thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.confirmCode(MILL_ID, PHONE, "PAYMENT:12:9999.00", "123456"))
                    .hasMessageContaining("OTP expired or not requested");
        }
    }

    @Nested
    class ConsumeToken {
        private PhoneOtp verified(LocalDateTime verifiedAt) {
            PhoneOtp o = otp("123456", verifiedAt, verifiedAt.plusMinutes(5));
            o.setVerifiedAt(verifiedAt);
            o.setVerificationToken("tok");
            return o;
        }

        @Test
        void validTokenIsConsumedOnce() {
            PhoneOtp o = verified(LocalDateTime.now());
            when(otpRepository.findByVerificationToken("tok")).thenReturn(Optional.of(o));

            service.consumeToken(MILL_ID, PHONE, "tok");

            assertThat(o.getConsumedAt()).isNotNull();
            assertThatThrownBy(() -> service.consumeToken(MILL_ID, PHONE, "tok"))
                    .hasMessageContaining("not verified");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"unknown"})
        void missingOrUnknownTokenIsRejected(String token) {
            assertThatThrownBy(() -> service.consumeToken(MILL_ID, PHONE, token))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not verified");
        }

        @Test
        void tokenForAnotherNumberIsRejected() {
            when(otpRepository.findByVerificationToken("tok")).thenReturn(Optional.of(verified(LocalDateTime.now())));
            assertThatThrownBy(() -> service.consumeToken(MILL_ID, "9000000000", "tok")).hasMessageContaining("not verified");
        }

        @Test
        void tokenFromAnotherMillIsRejected() {
            when(otpRepository.findByVerificationToken("tok")).thenReturn(Optional.of(verified(LocalDateTime.now())));
            assertThatThrownBy(() -> service.consumeToken(99L, PHONE, "tok")).hasMessageContaining("not verified");
        }

        @Test
        void expiredTokenIsRejected() {
            when(otpRepository.findByVerificationToken("tok")).thenReturn(Optional.of(verified(LocalDateTime.now().minusMinutes(16))));
            assertThatThrownBy(() -> service.consumeToken(MILL_ID, PHONE, "tok")).hasMessageContaining("not verified");
        }
    
        @Test
        void tokenFromAnotherPurposeIsRejected() {
            PhoneOtp o = verified(LocalDateTime.now());
            o.setPurpose("PAYMENT:12:5000.00");
            when(otpRepository.findByVerificationToken("tok")).thenReturn(Optional.of(o));
            assertThatThrownBy(() -> service.consumeToken(MILL_ID, PHONE, "tok")).hasMessageContaining("not verified");
        }
    }
}
