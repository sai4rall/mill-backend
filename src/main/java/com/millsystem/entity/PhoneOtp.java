package com.millsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * One OTP challenge sent to a mobile number. Once verified it yields a single-use
 * verification token that must accompany the farmer create/update request.
 */
@Entity
@Table(name = "phone_otps", indexes = {
        @Index(name = "idx_otp_mill_phone", columnList = "mill_id, phone, created_at"),
        @Index(name = "idx_otp_purpose", columnList = "mill_id, phone, purpose"),
        @Index(name = "uk_otp_token", columnList = "verification_token", unique = true)
})
public class PhoneOtp {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mill_id", nullable = false)
    private Mill mill;

    @Column(nullable = false, length = 15)
    private String phone;

    // What the code is for, e.g. PHONE_VERIFY or PAYMENT:<purchaseId>:<amount>. A code only works for its own purpose.
    @org.hibernate.annotations.ColumnDefault("'PHONE_VERIFY'")
    @Column(nullable = false, length = 100)
    private String purpose = "PHONE_VERIFY";

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_token", length = 64)
    private String verificationToken;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Mill getMill() { return mill; }
    public void setMill(Mill mill) { this.mill = mill; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
