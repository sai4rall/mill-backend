package com.millsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "truck_locations", indexes = {
        @Index(name = "idx_truck_mill", columnList = "mill_id, truck_number"),
        @Index(name = "idx_truck_time", columnList = "truck_number, recorded_at")
})
public class TruckLocation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mill_id", nullable = false)
    private Mill mill;

    @Column(name = "truck_number", nullable = false, length = 20)
    private String truckNumber;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(precision = 6, scale = 2)
    private BigDecimal speed;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Mill getMill() { return mill; }
    public void setMill(Mill mill) { this.mill = mill; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
}
