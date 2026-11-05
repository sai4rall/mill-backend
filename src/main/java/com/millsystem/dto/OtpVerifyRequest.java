package com.millsystem.dto;

import jakarta.validation.constraints.NotBlank;

public class OtpVerifyRequest {
    @NotBlank private String phone;
    @NotBlank private String otp;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
