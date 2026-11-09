package com.millsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.millsystem.entity.MachineDowntime;
import com.millsystem.entity.Shift;
import com.millsystem.validation.NotFutureDate;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** A machine stoppage. Times are "HH:mm"; an end time before the start means it ran past midnight. */
public class DowntimeDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private Long id;

    @NotNull(message = "Date is required")
    @NotFutureDate(message = "Date cannot be in the future")
    private LocalDate downtimeDate;

    @NotNull(message = "Shift is required")
    private Shift shift;

    @NotBlank(message = "Machine is required")
    @Size(max = 100, message = "Machine must be at most 100 characters")
    private String machine;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Reason is required")
    private MachineDowntime.Reason reason;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private Integer durationMinutes;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private String recordedBy;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDateTime createdAt;

    public static DowntimeDto from(MachineDowntime d) {
        DowntimeDto x = new DowntimeDto();
        x.id = d.getId(); x.downtimeDate = d.getDowntimeDate(); x.shift = d.getShift(); x.machine = d.getMachine();
        x.startTime = d.getStartTime(); x.endTime = d.getEndTime(); x.reason = d.getReason(); x.notes = d.getNotes();
        x.durationMinutes = d.getDurationMinutes(); x.recordedBy = d.getRecordedBy(); x.createdAt = d.getCreatedAt();
        return x;
    }

    @JsonIgnore
    @AssertTrue(message = "End time must be different from the start time")
    public boolean isTimesValid() { return startTime == null || endTime == null || !startTime.equals(endTime); }

    @JsonIgnore
    @AssertTrue(message = "Describe the reason in notes when the reason is Other")
    public boolean isReasonExplained() { return reason != MachineDowntime.Reason.OTHER || (notes != null && !notes.isBlank()); }

    public Long getId() { return id; }
    public LocalDate getDowntimeDate() { return downtimeDate; }
    public void setDowntimeDate(LocalDate d) { this.downtimeDate = d; }
    public Shift getShift() { return shift; }
    public void setShift(Shift shift) { this.shift = shift; }
    public String getMachine() { return machine; }
    public void setMachine(String machine) { this.machine = machine == null ? null : machine.trim().replaceAll("\\s+", " "); }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime t) { this.startTime = t; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime t) { this.endTime = t; }
    public MachineDowntime.Reason getReason() { return reason; }
    public void setReason(MachineDowntime.Reason reason) { this.reason = reason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes == null || notes.isBlank() ? null : notes.trim(); }
    public Integer getDurationMinutes() { return durationMinutes; }
    public String getRecordedBy() { return recordedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
