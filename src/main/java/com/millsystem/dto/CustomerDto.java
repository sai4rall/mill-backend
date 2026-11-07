package com.millsystem.dto;

import jakarta.validation.constraints.*;
import com.millsystem.entity.Customer;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class CustomerDto {
    private Long id;
    @NotBlank @Size(max = 255, message = "Must be at most 255 characters") private String name;
    @Pattern(regexp = "^\\+?[0-9][0-9 ()-]{6,19}$", message = "Enter a valid phone number")
    private String phone;
    @Email(message = "Enter a valid email address") @Size(max = 255, message = "Must be at most 255 characters")
    private String email;
    @Size(max = 2000, message = "Must be at most 2000 characters")
    private String address;
    @Size(max = 255, message = "Must be at most 255 characters")
    private String companyName;
    private LocalDateTime createdAt;

    public static CustomerDto from(Customer c) {
        CustomerDto dto = new CustomerDto();
        dto.id = c.getId(); dto.name = c.getName(); dto.phone = c.getPhone();
        dto.email = c.getEmail(); dto.address = c.getAddress();
        dto.companyName = c.getCompanyName(); dto.createdAt = c.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    private static String blankToNull(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    public void setName(String name) { this.name = name == null ? null : name.trim(); }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = blankToNull(phone); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = blankToNull(email); }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = blankToNull(address); }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = blankToNull(companyName); }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
