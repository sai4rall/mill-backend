package com.millsystem.dto;

import com.millsystem.entity.Customer;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class CustomerDto {
    private Long id;
    @NotBlank private String name;
    private String phone;
    private String email;
    private String address;
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
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
