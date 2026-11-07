package com.millsystem.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GpsPingDto {
    /** Optional and only cross-checked: the mill comes from the device key (X-Device-Key header). */
    private Long millId;
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9 -]{1,20}$", message = "Truck number: up to 20 letters, digits, spaces or hyphens")
    private String truckNumber;
    @NotNull @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90") @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;
    @NotNull @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180") @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;
    @DecimalMin(value = "0", message = "Speed cannot be negative") @DecimalMax(value = "300", message = "Speed must be at most 300 km/h")
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
