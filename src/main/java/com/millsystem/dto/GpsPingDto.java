package com.millsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GpsPingDto {
    @NotNull  private Long millId;
    @NotBlank private String truckNumber;
    @NotNull  private BigDecimal latitude;
    @NotNull  private BigDecimal longitude;
    private BigDecimal speed;
    private LocalDateTime recordedAt;

    public Long getMillId() { return millId; }
    public void setMillId(Long millId) { this.millId = millId; }
    public String getTruckNumber() { return truckNumber; }
    public void setTruckNumber(String truckNumber) { this.truckNumber = truckNumber; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public BigDecimal getSpeed() { return speed; }
    public void setSpeed(BigDecimal speed) { this.speed = speed; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
