package com.millsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.millsystem.entity.RejectedLorry;
import com.millsystem.validation.NotFutureDate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A rejected lorry. Farmer, weight and moisture are optional; a reason of OTHER needs notes. */
public class RejectedLorryDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private Long id;

    private Long farmerId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private String farmerName;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private String farmerType;

    @NotNull(message = "Date is required")
    @NotFutureDate(message = "Date cannot be in the future")
    private LocalDate rejectionDate;

    @NotBlank(message = "Truck number is required")
    @Size(max = 20, message = "Truck number must be at most 20 characters")
    private String truckNumber;

    @Positive(message = "Weight must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Weight can have at most 8 digits and 2 decimal places")
    private BigDecimal approxWeightQuintals;

    @DecimalMin(value = "0.00", message = "Moisture % must be between 0 and 100")
    @DecimalMax(value = "100.00", message = "Moisture % must be between 0 and 100")
    @Digits(integer = 3, fraction = 2, message = "Moisture % can have at most 2 decimals")
    private BigDecimal moisturePct;

    @NotNull(message = "Reason is required")
    private RejectedLorry.Reason reason;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private String recordedBy;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDateTime createdAt;

    public static RejectedLorryDto from(RejectedLorry r) {
        RejectedLorryDto d = new RejectedLorryDto();
        d.id = r.getId(); d.rejectionDate = r.getRejectionDate(); d.truckNumber = r.getTruckNumber();
        d.approxWeightQuintals = r.getApproxWeightQuintals(); d.moisturePct = r.getMoisturePct();
        d.reason = r.getReason(); d.notes = r.getNotes(); d.recordedBy = r.getRecordedBy(); d.createdAt = r.getCreatedAt();
        if (r.getFarmer() != null) {
            d.farmerId = r.getFarmer().getId(); d.farmerName = r.getFarmer().getName(); d.farmerType = r.getFarmer().getType().name();
        }
        return d;
    }

    /** "Other" says nothing on its own, so it needs a note. */
    @JsonIgnore
    @AssertTrue(message = "Describe the reason in notes when the reason is Other")
    public boolean isReasonExplained() {
        return reason != RejectedLorry.Reason.OTHER || (notes != null && !notes.isBlank());
    }

    public Long getId() { return id; }
    public Long getFarmerId() { return farmerId; }
    public void setFarmerId(Long farmerId) { this.farmerId = farmerId; }
    public String getFarmerName() { return farmerName; }
    public String getFarmerType() { return farmerType; }
    public LocalDate getRejectionDate() { return rejectionDate; }
    public void setRejectionDate(LocalDate rejectionDate) { this.rejectionDate = rejectionDate; }
    public String getTruckNumber() { return truckNumber; }
    public void setTruckNumber(String truckNumber) {
        this.truckNumber = truckNumber == null ? null : truckNumber.replaceAll("\\s+", "").toUpperCase();
    }
    public BigDecimal getApproxWeightQuintals() { return approxWeightQuintals; }
    public void setApproxWeightQuintals(BigDecimal approxWeightQuintals) { this.approxWeightQuintals = approxWeightQuintals; }
    public BigDecimal getMoisturePct() { return moisturePct; }
    public void setMoisturePct(BigDecimal moisturePct) { this.moisturePct = moisturePct; }
    public RejectedLorry.Reason getReason() { return reason; }
    public void setReason(RejectedLorry.Reason reason) { this.reason = reason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes == null || notes.isBlank() ? null : notes.trim(); }
    public String getRecordedBy() { return recordedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
