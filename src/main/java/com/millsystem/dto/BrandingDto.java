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

    public static BrandingDto from(Mill m) {
        BrandingDto dto = new BrandingDto();
        dto.millId = m.getId(); dto.millName = m.getName(); dto.location = m.getLocation();
        dto.status = m.getStatus().name(); dto.logoUpdatedAt = m.getLogoUpdatedAt();
        return dto;
    }

    public Long getMillId() { return millId; }
    public String getMillName() { return millName; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public LocalDateTime getLogoUpdatedAt() { return logoUpdatedAt; }
}
