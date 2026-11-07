package com.millsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "farmers", indexes = @Index(name = "idx_farmer_mill_type", columnList = "mill_id, party_type"))
public class Farmer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mill_id", nullable = false)
    private Mill mill;

    @Column(nullable = false)
    private String name;

    /**
     * Who we buy paddy from: a farmer, an agent (buys from farmers on the mill's behalf) or a supplier (trader).
     * Stored on its own so purchases can be reported per type (vendor-wise / agent-wise). Existing rows are farmers.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'FARMER'")
    private PartyType type = PartyType.FARMER;

    public enum PartyType { FARMER, AGENT, SUPPLIER }

    public PartyType getType() { return type; }
    public void setType(PartyType type) { this.type = type == null ? PartyType.FARMER : type; }

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "ifsc_code")
    private String ifscCode;

    // True once the current phone number has been confirmed via OTP; reset whenever the number changes.
    @ColumnDefault("false")
    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    // Set when a photo is uploaded (null = no photo). Also used by clients as a cache-busting version.
    @Column(name = "photo_updated_at")
    private LocalDateTime photoUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Mill getMill() { return mill; }
    public void setMill(Mill mill) { this.mill = mill; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }
    public LocalDateTime getPhotoUpdatedAt() { return photoUpdatedAt; }
    public void setPhotoUpdatedAt(LocalDateTime photoUpdatedAt) { this.photoUpdatedAt = photoUpdatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
