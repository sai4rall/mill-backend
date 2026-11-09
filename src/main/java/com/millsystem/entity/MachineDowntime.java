package com.millsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** A stoppage of a mill machine. A night stoppage may end after midnight (end time earlier than start time). */
@Entity
@Table(name = "machine_downtime", indexes = @Index(name = "idx_downtime_mill_date", columnList = "mill_id, downtime_date"))
public class MachineDowntime {

    public enum Reason { BREAKDOWN, POWER_CUT, MAINTENANCE, NO_PADDY, LABOUR_SHORTAGE, OTHER }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mill_id", nullable = false)
    private Mill mill;

    @Column(name = "downtime_date", nullable = false)
    private LocalDate downtimeDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private Shift shift;

    /** Machine name, e.g. "Husker", "Polisher", "Colour sorter". */
    @Column(nullable = false, length = 100)
    private String machine;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private Reason reason;

    @Column(length = 2000)
    private String notes;

    @Column(name = "recorded_by", length = 50)
    private String recordedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Minutes between start and end; an end before the start means the stoppage ran past midnight. */
    public static int minutesBetween(LocalTime start, LocalTime end) {
        int m = (end.toSecondOfDay() - start.toSecondOfDay()) / 60;
        return m < 0 ? m + 24 * 60 : m;
    }

    public Long getId() { return id; }
    public Mill getMill() { return mill; }
    public void setMill(Mill mill) { this.mill = mill; }
    public LocalDate getDowntimeDate() { return downtimeDate; }
    public void setDowntimeDate(LocalDate d) { this.downtimeDate = d; }
    public Shift getShift() { return shift; }
    public void setShift(Shift shift) { this.shift = shift; }
    public String getMachine() { return machine; }
    public void setMachine(String machine) { this.machine = machine; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime t) { this.startTime = t; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime t) { this.endTime = t; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int m) { this.durationMinutes = m; }
    public Reason getReason() { return reason; }
    public void setReason(Reason reason) { this.reason = reason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
