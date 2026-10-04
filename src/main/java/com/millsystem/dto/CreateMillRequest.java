package com.millsystem.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateMillRequest {
    @NotBlank private String name;
    private String location;
    private String contactPhone;
    private String contactEmail;
    @NotBlank private String adminUsername;
    private String adminFullName;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public String getAdminFullName() { return adminFullName; }
    public void setAdminFullName(String adminFullName) { this.adminFullName = adminFullName; }
}
