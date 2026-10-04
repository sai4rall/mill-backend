package com.millsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TruckDto {
    private String truckNumber;
    private BigDecimal lastLatitude;
    private BigDecimal lastLongitude;
    private BigDecimal lastSpeed;
    private LocalDateTime lastSeen;

    public TruckDto(String truckNumber, BigDecimal lastLatitude, BigDecimal lastLongitude,
                    BigDecimal lastSpeed, LocalDateTime lastSeen) {
        this.truckNumber = truckNumber;
        this.lastLatitude = lastLatitude;
        this.lastLongitude = lastLongitude;
        this.lastSpeed = lastSpeed;
        this.lastSeen = lastSeen;
    }

    public String getTruckNumber() { return truckNumber; }
    public BigDecimal getLastLatitude() { return lastLatitude; }
    public BigDecimal getLastLongitude() { return lastLongitude; }
    public BigDecimal getLastSpeed() { return lastSpeed; }
    public LocalDateTime getLastSeen() { return lastSeen; }
}
