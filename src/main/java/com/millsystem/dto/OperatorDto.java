package com.millsystem.dto;

import com.millsystem.entity.User;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class OperatorDto {
    private Long id;
    @NotBlank private String username;
    private String fullName;
    private boolean active;
    private LocalDateTime createdAt;
    private String generatedPassword;

    public static OperatorDto from(User u) {
        OperatorDto dto = new OperatorDto();
        dto.id = u.getId(); dto.username = u.getUsername(); dto.fullName = u.getFullName();
        dto.active = u.isActive(); dto.createdAt = u.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getGeneratedPassword() { return generatedPassword; }
    public void setGeneratedPassword(String generatedPassword) { this.generatedPassword = generatedPassword; }
}
