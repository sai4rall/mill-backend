package com.millsystem.dto;

import jakarta.validation.constraints.*;
import com.millsystem.validation.NotFutureDate;
import com.millsystem.entity.ExportOrder;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExportOrderDto {
    private Long id;
    @NotNull private Long customerId;
    private String customerName;
    @NotNull @NotFutureDate(message = "Order date cannot be in the future") private LocalDate orderDate;
    @Size(max = 100, message = "Must be at most 100 characters")
    private String riceType;
    @NotNull @Positive @Digits(integer = 8, fraction = 2, message = "Quantity can have at most 8 digits and 2 decimal places") private BigDecimal quantityQuintals;
    @PositiveOrZero @Digits(integer = 8, fraction = 2, message = "Price can have at most 8 digits and 2 decimal places")
    private BigDecimal pricePerQuintal;
    private BigDecimal totalAmount;
    private ExportOrder.OrderStatus status;
    private BigDecimal shippedQuintals;
    private BigDecimal remainingQuintals;
    @Size(max = 2000, message = "Must be at most 2000 characters")
    private String notes;
    private LocalDateTime createdAt;

    public static ExportOrderDto from(ExportOrder o, BigDecimal shippedQuintals) {
        ExportOrderDto dto = new ExportOrderDto();
        dto.id = o.getId(); dto.customerId = o.getCustomer().getId(); dto.customerName = o.getCustomer().getName();
        dto.orderDate = o.getOrderDate(); dto.riceType = o.getRiceType(); dto.quantityQuintals = o.getQuantityQuintals();
        dto.pricePerQuintal = o.getPricePerQuintal(); dto.totalAmount = o.getTotalAmount();
        dto.status = o.getStatus(); dto.shippedQuintals = shippedQuintals;
        dto.remainingQuintals = o.getQuantityQuintals().subtract(shippedQuintals);
        dto.notes = o.getNotes(); dto.createdAt = o.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public String getRiceType() { return riceType; }
    public void setRiceType(String riceType) { this.riceType = riceType; }
    public BigDecimal getQuantityQuintals() { return quantityQuintals; }
    public void setQuantityQuintals(BigDecimal quantityQuintals) { this.quantityQuintals = quantityQuintals; }
    public BigDecimal getPricePerQuintal() { return pricePerQuintal; }
    public void setPricePerQuintal(BigDecimal pricePerQuintal) { this.pricePerQuintal = pricePerQuintal; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public ExportOrder.OrderStatus getStatus() { return status; }
    public BigDecimal getShippedQuintals() { return shippedQuintals; }
    public BigDecimal getRemainingQuintals() { return remainingQuintals; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
