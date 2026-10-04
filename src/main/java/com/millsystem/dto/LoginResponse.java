package com.millsystem.dto;

public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private Long millId;
    private String fullName;

    public LoginResponse(String token, String username, String role, Long millId, String fullName) {
        this.token = token; this.username = username; this.role = role;
        this.millId = millId; this.fullName = fullName;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public Long getMillId() { return millId; }
    public String getFullName() { return fullName; }
}
