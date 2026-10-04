package com.millsystem.dto;

public class CreateMillResponse {
    private Long millId;
    private String millName;
    private String adminUsername;
    private String generatedPassword;
    private String message;

    public CreateMillResponse(Long millId, String millName, String adminUsername, String generatedPassword, String message) {
        this.millId = millId; this.millName = millName; this.adminUsername = adminUsername;
        this.generatedPassword = generatedPassword; this.message = message;
    }

    public Long getMillId() { return millId; }
    public String getMillName() { return millName; }
    public String getAdminUsername() { return adminUsername; }
    public String getGeneratedPassword() { return generatedPassword; }
    public String getMessage() { return message; }
}
