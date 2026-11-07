package com.millsystem.dto;

import com.millsystem.entity.Mill;
import java.time.LocalDateTime;

/** What the app shell needs to brand itself for a mill. {@code logoUpdatedAt} is null when there is no logo. */
public class BrandingDto {
    private Long millId;
    private String millName;
    private String location;
    private String status;
    private LocalDateTime logoUpdatedAt;
    // Dev admin listing only: the owning company (super admin). Not sent to mill users.
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Long ownerId;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String ownerUsername;

    public static BrandingDto from(Mill m) {
        BrandingDto dto = new BrandingDto();
        dto.millId = m.getId(); dto.millName = m.getName(); dto.location = m.getLocation();
        dto.status = m.getStatus().name(); dto.logoUpdatedAt = m.getLogoUpdatedAt();
        return dto;
    }

    /** For the dev admin: includes the owning super admin. */
    public static BrandingDto withOwner(Mill m) {
        BrandingDto dto = from(m);
        if (m.getOwner() != null) { dto.ownerId = m.getOwner().getId(); dto.ownerUsername = m.getOwner().getUsername(); }
        return dto;
    }

    public Long getOwnerId() { return ownerId; }
    public String getOwnerUsername() { return ownerUsername; }
    public Long getMillId() { return millId; }
    public String getMillName() { return millName; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public LocalDateTime getLogoUpdatedAt() { return logoUpdatedAt; }
}
