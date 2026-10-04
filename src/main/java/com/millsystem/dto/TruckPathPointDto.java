package com.millsystem.dto;

import com.millsystem.entity.TruckLocation;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TruckPathPointDto {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal speed;
    private LocalDateTime recordedAt;

    public static TruckPathPointDto from(TruckLocation t) {
        TruckPathPointDto dto = new TruckPathPointDto();
        dto.latitude = t.getLatitude();
        dto.longitude = t.getLongitude();
        dto.speed = t.getSpeed();
        dto.recordedAt = t.getRecordedAt();
        return dto;
    }

    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public BigDecimal getSpeed() { return speed; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}
