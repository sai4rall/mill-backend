package com.millsystem.service;

import com.millsystem.config.AppTime;
import com.millsystem.dto.ProductionReportDto;
import com.millsystem.dto.ProductionReportDto.DowntimeGroup;
import com.millsystem.dto.ProductionReportDto.Row;
import com.millsystem.entity.MachineDowntime;
import com.millsystem.entity.Mill;
import com.millsystem.entity.MillBatch;
import com.millsystem.entity.Shift;
import com.millsystem.repository.MachineDowntimeRepository;
import com.millsystem.repository.MillBatchRepository;
import com.millsystem.repository.MillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

/**
 * Production monitoring: daily output, raw material in vs rice out, milling recovery, losses (unaccounted weight and
 * shortfall against the expected rice), machine downtime, and comparisons across duty shifts and across mills.
 */
@Service
public class ProductionService {

    public static final int MAX_DAYS = 366;
    public static final String NO_SHIFT = "NOT_SET";

    private final MillBatchRepository batches;
    private final MachineDowntimeRepository downtime;
    private final MillRepository mills;

    public ProductionService(MillBatchRepository batches, MachineDowntimeRepository downtime, MillRepository mills) {
        this.batches = batches;
        this.downtime = downtime;
        this.mills = mills;
    }

    /** Defaults to the last 30 days; the period may not exceed a year or end in the future. */
    public static LocalDate[] period(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : AppTime.today();
        LocalDate start = from != null ? from : end.minusDays(29);
        if (start.isAfter(end)) throw new IllegalArgumentException("From date must be on or before the to date");
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) throw new IllegalArgumentException("The period can be at most " + MAX_DAYS + " days");
        return new LocalDate[]{start, end};
    }

    @Transactional(readOnly = true)
    public ProductionReportDto report(Long millId, LocalDate from, LocalDate to) {
        LocalDate[] p = period(from, to);
        ProductionReportDto r = new ProductionReportDto();
        r.setFrom(p[0]); r.setTo(p[1]);

        Map<LocalDate, Row> days = new TreeMap<>();
        for (LocalDate d = p[0]; !d.isAfter(p[1]); d = d.plusDays(1)) days.put(d, new Row(d.toString()));
        Map<String, Row> shifts = new LinkedHashMap<>();
        for (Shift s : Shift.values()) shifts.put(s.name(), new Row(s.name()));

        int inProgress = 0;
        for (MillBatch b : batches.findByMillIdAndBatchDateBetweenOrderByBatchDateAsc(millId, p[0], p[1])) {
            if (b.getStatus() != MillBatch.BatchStatus.COMPLETED) { inProgress++; continue; }
            String shift = b.getShift() == null ? NO_SHIFT : b.getShift().name();
            for (Row row : List.of(r.getTotals(), days.get(b.getBatchDate()), shifts.computeIfAbsent(shift, Row::new))) add(row, b);
        }
        r.setBatchesInProgress(inProgress);

        List<MachineDowntime> stops = downtime.findByMillIdAndDowntimeDateBetween(millId, p[0], p[1]);
        for (MachineDowntime d : stops) {
            r.getTotals().addDowntime(d.getDurationMinutes());
            days.get(d.getDowntimeDate()).addDowntime(d.getDurationMinutes());
            shifts.computeIfAbsent(d.getShift().name(), Row::new).addDowntime(d.getDurationMinutes());
        }
        r.getDays().addAll(days.values());
        r.getShifts().addAll(shifts.values());
        r.getDowntimeByReason().addAll(group(stops, d -> d.getReason().name()));
        r.getDowntimeByMachine().addAll(group(stops, MachineDowntime::getMachine));
        return r;
    }

    /** Super admin: one row per mill of their company for the period (key = mill name), busiest first. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> compareMills(Long ownerId, LocalDate from, LocalDate to) {
        LocalDate[] p = period(from, to);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Mill m : mills.findByOwnerIdOrderByNameAsc(ownerId)) {
            Row totals = report(m.getId(), p[0], p[1]).getTotals();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("millId", m.getId()); row.put("millName", m.getName()); row.put("status", m.getStatus().name());
            row.put("from", p[0]); row.put("to", p[1]); row.put("totals", totals);
            out.add(row);
        }
        out.sort(Comparator.comparing((Map<String, Object> x) -> ((Row) x.get("totals")).getPaddyInQuintals()).reversed());
        return out;
    }

    private static void add(Row row, MillBatch b) {
        BigDecimal shortfall = null;
        if (b.getExpectedRiceQuintals() != null && b.getActualRiceQuintals() != null) {
            BigDecimal gap = b.getExpectedRiceQuintals().subtract(b.getActualRiceQuintals());
            if (gap.signum() > 0) shortfall = gap;
        }
        row.addBatch();
        row.add(b.getPaddyInputQuintals(), b.getActualRiceQuintals(), b.getBrokenRiceQuintals(), b.getBranQuintals(),
                b.getHuskQuintals(), b.getUnaccountedQuintals(), shortfall);
    }

    private static List<DowntimeGroup> group(List<MachineDowntime> stops, Function<MachineDowntime, String> key) {
        Map<String, int[]> g = new LinkedHashMap<>();
        for (MachineDowntime d : stops) {
            int[] v = g.computeIfAbsent(key.apply(d), k -> new int[2]);
            v[0]++; v[1] += d.getDurationMinutes();
        }
        return g.entrySet().stream().map(e -> new DowntimeGroup(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingInt(DowntimeGroup::minutes).reversed()).toList();
    }
}
