package com.millsystem.dto;

import jakarta.validation.constraints.NotBlank;

public class OtpSendRequest {
    @NotBlank private String phone;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
