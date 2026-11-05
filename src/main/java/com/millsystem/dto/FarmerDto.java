package com.millsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.millsystem.entity.Farmer;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Farmer request/response body. Setters normalise incoming values (trim, blank → null,
 * IFSC upper-cased, spaces removed from account numbers) before Bean Validation runs.
 * The phone number is validated and normalised by {@code OtpService.normalizePhone} in the service,
 * because an unchanged legacy number must still be accepted on update.
 * Keep these rules in sync with frontend/src/utils/farmerValidation.js.
 */
public class FarmerDto {
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    @Pattern(regexp = "^[\\p{L}\\p{M}][\\p{L}\\p{M} .'-]*$",
            message = "Name can contain only letters, spaces and . ' -")
    private String name;

    private String phone;

    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;

    @Pattern(regexp = "^\\d{9,18}$", message = "Bank account number must be 9-18 digits")
    private String bankAccount;

    @Size(min = 2, max = 100, message = "Bank name must be 2-100 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z .&()'-]*$",
            message = "Bank name can contain only letters, spaces and . & ( ) ' -")
    private String bankName;

    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC code must be 11 characters, e.g. SBIN0001234")
    private String ifscCode;

    private boolean phoneVerified;
    private LocalDateTime photoUpdatedAt;
    private LocalDateTime createdAt;
    // Returned by POST /api/farmers/otp/verify; required when registering or changing the phone number.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String phoneVerificationToken;

    public static FarmerDto from(Farmer f) {
        FarmerDto dto = new FarmerDto();
        dto.id = f.getId(); dto.name = f.getName(); dto.phone = f.getPhone();
        dto.address = f.getAddress(); dto.bankAccount = f.getBankAccount();
        dto.bankName = f.getBankName(); dto.ifscCode = f.getIfscCode(); dto.createdAt = f.getCreatedAt();
        dto.phoneVerified = f.isPhoneVerified(); dto.photoUpdatedAt = f.getPhotoUpdatedAt();
        return dto;
    }

    /** Bank account number, bank name and IFSC are all required together, or all left empty. */
    @JsonIgnore
    @AssertTrue(message = "Bank account number, bank name and IFSC code must be provided together")
    public boolean isBankDetailsComplete() {
        int filled = (bankAccount != null ? 1 : 0) + (bankName != null ? 1 : 0) + (ifscCode != null ? 1 : 0);
        return filled == 0 || filled == 3;
    }

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = clean(name); }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone == null ? null : phone.trim(); }
    public String getAddress() { return address; }
    public void setAddress(String address) {
        String t = address == null ? null : address.trim();
        this.address = (t == null || t.isEmpty()) ? null : t;
    }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) {
        String t = bankAccount == null ? null : bankAccount.replaceAll("\\s+", "");
        this.bankAccount = (t == null || t.isEmpty()) ? null : t;
    }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = clean(bankName); }
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) {
        String t = ifscCode == null ? null : ifscCode.replaceAll("\\s+", "").toUpperCase();
        this.ifscCode = (t == null || t.isEmpty()) ? null : t;
    }
    public boolean isPhoneVerified() { return phoneVerified; }
    public LocalDateTime getPhotoUpdatedAt() { return photoUpdatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getPhoneVerificationToken() { return phoneVerificationToken; }
    public void setPhoneVerificationToken(String phoneVerificationToken) { this.phoneVerificationToken = phoneVerificationToken; }
}
