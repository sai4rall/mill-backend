package com.millsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "export_shipments")
public class ExportShipment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mill_id", nullable = false)
    private Mill mill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ExportOrder order;

    @Column(name = "shipment_date", nullable = false)
    private LocalDate shipmentDate;

    @Column(name = "quintals_exported", nullable = false, precision = 10, scale = 2)
    private BigDecimal quintalsExported;

    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Mill getMill() { return mill; }
    public void setMill(Mill mill) { this.mill = mill; }
    public ExportOrder getOrder() { return order; }
    public void setOrder(ExportOrder order) { this.order = order; }
    public LocalDate getShipmentDate() { return shipmentDate; }
    public void setShipmentDate(LocalDate shipmentDate) { this.shipmentDate = shipmentDate; }
    public BigDecimal getQuintalsExported() { return quintalsExported; }
    public void setQuintalsExported(BigDecimal quintalsExported) { this.quintalsExported = quintalsExported; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
