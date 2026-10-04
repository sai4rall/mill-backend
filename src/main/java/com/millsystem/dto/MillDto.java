package com.millsystem.dto;

import com.millsystem.entity.Mill;
import java.time.LocalDateTime;

public class MillDto {
    private Long id;
    private String name;
    private String location;
    private String contactPhone;
    private String contactEmail;
    private Mill.MillStatus status;
    private LocalDateTime createdAt;
    private String adminUsername;

    public static MillDto from(Mill mill, String adminUsername) {
        MillDto dto = new MillDto();
        dto.id = mill.getId(); dto.name = mill.getName(); dto.location = mill.getLocation();
        dto.contactPhone = mill.getContactPhone(); dto.contactEmail = mill.getContactEmail();
        dto.status = mill.getStatus(); dto.createdAt = mill.getCreatedAt(); dto.adminUsername = adminUsername;
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getContactPhone() { return contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public Mill.MillStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getAdminUsername() { return adminUsername; }
}
