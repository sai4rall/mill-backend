package com.millsystem.dto;

import com.millsystem.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class SuperAdminDto {
    private Long id;

    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$", message = "Username must be 3-50 characters: letters, digits, . _ -")
    private String username;

    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    private boolean active;
    private LocalDateTime createdAt;
    // Only set in the create / reset-password response; shown once.
    private String generatedPassword;

    public static SuperAdminDto from(User u) {
        SuperAdminDto dto = new SuperAdminDto();
        dto.id = u.getId(); dto.username = u.getUsername(); dto.fullName = u.getFullName();
        dto.active = u.isActive(); dto.createdAt = u.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username == null ? null : username.trim(); }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName == null || fullName.isBlank() ? null : fullName.trim(); }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getGeneratedPassword() { return generatedPassword; }
    public void setGeneratedPassword(String generatedPassword) { this.generatedPassword = generatedPassword; }
}
