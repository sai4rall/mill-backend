package com.millsystem.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Production for a period: totals, one row per day, one row per duty shift, and downtime by reason and machine.
 * Output figures come from batches completed in the period (by batch date).
 */
public class ProductionReportDto {

    /** One group of batches (a day, a shift, a mill or the whole period). */
    public static class Row {
        private String key;
        private int batches;
        private BigDecimal paddyInQuintals = BigDecimal.ZERO;
        private BigDecimal riceQuintals = BigDecimal.ZERO;
        private BigDecimal brokenRiceQuintals = BigDecimal.ZERO;
        private BigDecimal branQuintals = BigDecimal.ZERO;
        private BigDecimal huskQuintals = BigDecimal.ZERO;
        private BigDecimal unaccountedQuintals = BigDecimal.ZERO;
        private BigDecimal shortfallQuintals = BigDecimal.ZERO;
        private int downtimeMinutes;

        public Row() {}
        public Row(String key) { this.key = key; }

        /** Rice out as a % of paddy in (milling recovery); null when nothing was milled. */
        public BigDecimal getRecoveryPct() {
            return paddyInQuintals.signum() == 0 ? null
                    : riceQuintals.multiply(BigDecimal.valueOf(100)).divide(paddyInQuintals, 2, RoundingMode.HALF_UP);
        }
        /** Unaccounted weight as a % of paddy in. */
        public BigDecimal getUnaccountedPct() {
            return paddyInQuintals.signum() == 0 ? null
                    : unaccountedQuintals.multiply(BigDecimal.valueOf(100)).divide(paddyInQuintals, 2, RoundingMode.HALF_UP);
        }

        public String getKey() { return key; }
        public int getBatches() { return batches; }
        public void addBatch() { batches++; }
        public BigDecimal getPaddyInQuintals() { return paddyInQuintals; }
        public BigDecimal getRiceQuintals() { return riceQuintals; }
        public BigDecimal getBrokenRiceQuintals() { return brokenRiceQuintals; }
        public BigDecimal getBranQuintals() { return branQuintals; }
        public BigDecimal getHuskQuintals() { return huskQuintals; }
        public BigDecimal getUnaccountedQuintals() { return unaccountedQuintals; }
        public BigDecimal getShortfallQuintals() { return shortfallQuintals; }
        public int getDowntimeMinutes() { return downtimeMinutes; }
        public void add(BigDecimal paddy, BigDecimal rice, BigDecimal broken, BigDecimal bran, BigDecimal husk, BigDecimal unaccounted, BigDecimal shortfall) {
            paddyInQuintals = paddyInQuintals.add(nz(paddy)); riceQuintals = riceQuintals.add(nz(rice));
            brokenRiceQuintals = brokenRiceQuintals.add(nz(broken)); branQuintals = branQuintals.add(nz(bran)); huskQuintals = huskQuintals.add(nz(husk));
            unaccountedQuintals = unaccountedQuintals.add(nz(unaccounted)); shortfallQuintals = shortfallQuintals.add(nz(shortfall));
        }
        public void addDowntime(int minutes) { downtimeMinutes += minutes; }
        private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    }

    /** Downtime grouped by a reason or machine. */
    public record DowntimeGroup(String key, int stoppages, int minutes) {}

    private LocalDate from;
    private LocalDate to;
    private Row totals = new Row("TOTAL");
    private int batchesInProgress;
    private List<Row> days = new ArrayList<>();
    private List<Row> shifts = new ArrayList<>();
    private List<DowntimeGroup> downtimeByReason = new ArrayList<>();
    private List<DowntimeGroup> downtimeByMachine = new ArrayList<>();

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public Row getTotals() { return totals; }
    public int getBatchesInProgress() { return batchesInProgress; }
    public void setBatchesInProgress(int n) { this.batchesInProgress = n; }
    public List<Row> getDays() { return days; }
    public List<Row> getShifts() { return shifts; }
    public List<DowntimeGroup> getDowntimeByReason() { return downtimeByReason; }
    public List<DowntimeGroup> getDowntimeByMachine() { return downtimeByMachine; }
}
