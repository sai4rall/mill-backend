package com.millsystem.dto;

import jakarta.validation.constraints.*;
import com.millsystem.validation.NotFutureDate;
import com.millsystem.entity.ExportShipment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ShipmentDto {
    private Long id;
    @NotNull private Long orderId;
    @NotNull @NotFutureDate(message = "Shipment date cannot be in the future") private LocalDate shipmentDate;
    @NotNull @Positive @Digits(integer = 8, fraction = 2, message = "Quantity can have at most 8 digits and 2 decimal places") private BigDecimal quintalsExported;
    @Size(max = 20, message = "Must be at most 20 characters")
    private String vehicleNumber;
    @Size(max = 2000, message = "Must be at most 2000 characters")
    private String notes;
    private LocalDateTime createdAt;
    private String customerName;   // read-only, for the shipments list
    private String riceType;       // read-only

    public static ShipmentDto from(ExportShipment s) {
        ShipmentDto dto = new ShipmentDto();
        dto.id = s.getId(); dto.orderId = s.getOrder().getId(); dto.shipmentDate = s.getShipmentDate();
        dto.quintalsExported = s.getQuintalsExported(); dto.vehicleNumber = s.getVehicleNumber();
        dto.notes = s.getNotes(); dto.createdAt = s.getCreatedAt();
        if (s.getOrder() != null) {
            dto.orderId = s.getOrder().getId();
            if (s.getOrder().getCustomer() != null) dto.customerName = s.getOrder().getCustomer().getName();
            dto.riceType = s.getOrder().getRiceType();
        }
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
    public String getCustomerName() { return customerName; }
    public String getRiceType() { return riceType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
