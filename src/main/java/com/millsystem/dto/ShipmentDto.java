package com.millsystem.dto;

import com.millsystem.entity.ExportShipment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ShipmentDto {
    private Long id;
    @NotNull private Long orderId;
    @NotNull private LocalDate shipmentDate;
    @NotNull @Positive private BigDecimal quintalsExported;
    private String vehicleNumber;
    private String notes;
    private LocalDateTime createdAt;

    public static ShipmentDto from(ExportShipment s) {
        ShipmentDto dto = new ShipmentDto();
        dto.id = s.getId(); dto.orderId = s.getOrder().getId(); dto.shipmentDate = s.getShipmentDate();
        dto.quintalsExported = s.getQuintalsExported(); dto.vehicleNumber = s.getVehicleNumber();
        dto.notes = s.getNotes(); dto.createdAt = s.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
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
