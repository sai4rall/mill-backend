package com.millsystem.dto;

import com.millsystem.entity.Farmer;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class FarmerDto {
    private Long id;
    @NotBlank private String name;
    private String phone;
    private String address;
    private String bankAccount;
    private String bankName;
    private String ifscCode;
    private LocalDateTime createdAt;

    public static FarmerDto from(Farmer f) {
        FarmerDto dto = new FarmerDto();
        dto.id = f.getId(); dto.name = f.getName(); dto.phone = f.getPhone();
        dto.address = f.getAddress(); dto.bankAccount = f.getBankAccount();
        dto.bankName = f.getBankName(); dto.ifscCode = f.getIfscCode(); dto.createdAt = f.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
}
