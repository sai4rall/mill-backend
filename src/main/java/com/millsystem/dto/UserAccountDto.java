package com.millsystem.dto;

import com.millsystem.entity.User;

import java.time.LocalDateTime;

/** Any user account as the dev admin sees it (no password data). */
public class UserAccountDto {
    private Long id;
    private String username;
    private String fullName;
    private String role;
    private Long millId;
    private String millName;
    private String millStatus;
    private boolean active;
    private LocalDateTime createdAt;

    public static UserAccountDto from(User u) {
        UserAccountDto dto = new UserAccountDto();
        dto.id = u.getId(); dto.username = u.getUsername(); dto.fullName = u.getFullName();
        dto.role = u.getRole().name(); dto.active = u.isActive(); dto.createdAt = u.getCreatedAt();
        if (u.getMill() != null) {
            dto.millId = u.getMill().getId(); dto.millName = u.getMill().getName(); dto.millStatus = u.getMill().getStatus().name();
        }
        return dto;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public Long getMillId() { return millId; }
    public String getMillName() { return millName; }
    public String getMillStatus() { return millStatus; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
