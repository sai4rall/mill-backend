package com.millsystem.service;

import com.millsystem.entity.Mill;
import com.millsystem.entity.PhoneOtp;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.PhoneOtpRepository;
import com.millsystem.sms.SmsSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mobile number verification by OTP, scoped per mill.
 * Flow: {@link #send} → {@link #verify} (returns a token) → {@link #consumeToken} when saving the farmer.
 */
@Service
public class OtpService {

    private static final int OTP_LENGTH = 6;
    public static final String PURPOSE_PHONE_VERIFY = "PHONE_VERIFY";

    private final PhoneOtpRepository otpRepository;
    private final MillRepository millRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsSender smsSender;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.otp.expiry-minutes:5}") private int expiryMinutes;
    @Value("${app.otp.resend-cooldown-seconds:30}") private int resendCooldownSeconds;
    @Value("${app.otp.max-attempts:5}") private int maxAttempts;
    @Value("${app.otp.max-sends-per-hour:5}") private int maxSendsPerHour;
    @Value("${app.otp.token-validity-minutes:15}") private int tokenValidityMinutes;

    public OtpService(PhoneOtpRepository otpRepository, MillRepository millRepository,
                      PasswordEncoder passwordEncoder, SmsSender smsSender) {
        this.otpRepository = otpRepository;
        this.millRepository = millRepository;
        this.passwordEncoder = passwordEncoder;
        this.smsSender = smsSender;
    }

    /**
     * Normalises an Indian mobile number to 10 digits, accepting +91 / 91 / 0 prefixes,
     * spaces and dashes. Throws if it is not a valid mobile number.
     */
    public static String normalizePhone(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("[\\s\\-()]", "");
        if (digits.startsWith("+91")) digits = digits.substring(3);
        else if (digits.length() == 12 && digits.startsWith("91")) digits = digits.substring(2);
        else if (digits.length() == 11 && digits.startsWith("0")) digits = digits.substring(1);
        if (!digits.matches("[6-9]\\d{9}")) {
            throw new IllegalArgumentException("Enter a valid 10-digit mobile number");
        }
        return digits;
    }

    /** Sends a phone-verification OTP. Returns the number of seconds until it expires. */
    @Transactional
    public int send(Long millId, String rawPhone) {
        String phone = normalizePhone(rawPhone);
        return sendCode(millId, phone, PURPOSE_PHONE_VERIFY, (code, mill) ->
                code + " is your OTP to verify your mobile number with " + mill.getName()
                        + ". Valid for " + expiryMinutes + " minutes. Do not share it with anyone.");
    }

    /** Builds the SMS text for a code. */
    @FunctionalInterface
    public interface OtpMessage {
        String build(String code, Mill mill);
    }

    /**
     * Sends a new OTP for a specific purpose to an already-normalised number, applying the resend cooldown
     * (per number + purpose) and the hourly limit (per number). Returns the number of seconds until it expires.
     */
    @Transactional
    public int sendCode(Long millId, String phone, String purpose, OtpMessage message) {
        Mill mill = millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        LocalDateTime now = LocalDateTime.now();

        // Resend cooldown is per number + purpose (a recent verification SMS must not block a payment OTP);
        // the hourly cap below counts every SMS to the number.
        otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(millId, phone, purpose).ifPresent(last -> {
            long waited = Duration.between(last.getCreatedAt(), now).getSeconds();
            if (waited < resendCooldownSeconds) {
                throw new IllegalArgumentException(
                        "Please wait " + (resendCooldownSeconds - waited) + "s before requesting another OTP");
            }
        });
        if (otpRepository.countByMillIdAndPhoneAndCreatedAtAfter(millId, phone, now.minusHours(1)) >= maxSendsPerHour) {
            throw new IllegalArgumentException("Too many OTP requests for this number. Try again later.");
        }

        String code = generateCode();
        PhoneOtp otp = new PhoneOtp();
        otp.setMill(mill);
        otp.setPhone(phone);
        otp.setPurpose(purpose);
        otp.setOtpHash(passwordEncoder.encode(code));
        otp.setExpiresAt(now.plusMinutes(expiryMinutes));
        otpRepository.save(otp);

        smsSender.send(phone, message.build(code, mill));
        return expiryMinutes * 60;
    }

    /**
     * Checks the OTP against the latest one sent to this number. On success returns a
     * single-use verification token. Failed attempts are counted, so this must not roll back on error.
     */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public String verify(Long millId, String rawPhone, String code) {
        PhoneOtp otp = checkCode(millId, normalizePhone(rawPhone), PURPOSE_PHONE_VERIFY, code);
        otp.setVerificationToken(UUID.randomUUID().toString());
        otpRepository.save(otp);
        return otp.getVerificationToken();
    }

    /**
     * Checks a code for a specific purpose and uses it up immediately (no token). Failed attempts are counted,
     * so callers must not roll back on IllegalArgumentException.
     */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void confirmCode(Long millId, String phone, String purpose, String code) {
        PhoneOtp otp = checkCode(millId, phone, purpose, code);
        otp.setConsumedAt(otp.getVerifiedAt());
        otpRepository.save(otp);
    }

    /** Validates the latest unused code for this number + purpose and marks it verified. */
    private PhoneOtp checkCode(Long millId, String phone, String purpose, String code) {
        PhoneOtp otp = otpRepository.findTopByMillIdAndPhoneAndPurposeOrderByCreatedAtDescIdDesc(millId, phone, purpose)
                .filter(o -> o.getVerifiedAt() == null && o.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("OTP expired or not requested. Please request a new OTP."));
        if (otp.getAttempts() >= maxAttempts) {
            throw new IllegalArgumentException("Too many incorrect attempts. Please request a new OTP.");
        }

        otp.setAttempts(otp.getAttempts() + 1);
        if (code == null || !passwordEncoder.matches(code.trim(), otp.getOtpHash())) {
            otpRepository.save(otp);
            int left = maxAttempts - otp.getAttempts();
            throw new IllegalArgumentException(left > 0
                    ? "Incorrect OTP. " + left + " attempt(s) left."
                    : "Incorrect OTP. Please request a new OTP.");
        }

        otp.setVerifiedAt(LocalDateTime.now());
        return otp;
    }

    /**
     * Marks a verification token as used. It must belong to this mill and (normalised) phone,
     * be unused, and have been issued within the token validity window.
     */
    @Transactional
    public void consumeToken(Long millId, String phone, String token) {
        PhoneOtp otp = (token == null || token.isBlank()) ? null
                : otpRepository.findByVerificationToken(token).orElse(null);
        boolean valid = otp != null
                && otp.getMill().getId().equals(millId)
                && otp.getPhone().equals(phone)
                && PURPOSE_PHONE_VERIFY.equals(otp.getPurpose())
                && otp.getConsumedAt() == null
                && otp.getVerifiedAt().plusMinutes(tokenValidityMinutes).isAfter(LocalDateTime.now());
        if (!valid) {
            throw new IllegalArgumentException("Mobile number is not verified. Please verify it with an OTP.");
        }
        otp.setConsumedAt(LocalDateTime.now());
        otpRepository.save(otp);
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        return String.format("%0" + OTP_LENGTH + "d", random.nextInt(bound));
    }
}
