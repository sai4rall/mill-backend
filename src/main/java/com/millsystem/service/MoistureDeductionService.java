package com.millsystem.service;

import com.millsystem.dto.MoistureRangeDto;
import com.millsystem.dto.MoistureTableDto;
import com.millsystem.entity.Mill;
import com.millsystem.entity.MoistureDeduction;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.MoistureDeductionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Moisture-based price deductions. Each super admin (company) maintains a default table for its own mills; a mill
 * admin may define the mill's own table, which then fully replaces the company default for that mill. Companies never
 * see or use each other's tables. Bands are fromPct ≤ m < toPct
 * (null toPct = no upper limit). Moisture outside every band — or no table at all — means no deduction.
 */
@Service
public class MoistureDeductionService {

    static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    static final int MAX_RANGES = 20;

    private final MoistureDeductionRepository repository;
    private final MillRepository millRepository;

    public MoistureDeductionService(MoistureDeductionRepository repository, MillRepository millRepository) {
        this.repository = repository;
        this.millRepository = millRepository;
    }

    /** The company default table of one super admin (empty for a mill with no owner). */
    @Transactional(readOnly = true)
    public List<MoistureRangeDto> getGlobal(Long ownerId) {
        if (ownerId == null) return List.of();
        return repository.findByMillIsNullAndOwnerIdOrderByFromPctAsc(ownerId).stream().map(MoistureRangeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public MoistureTableDto getForMill(Long millId) {
        List<MoistureRangeDto> mill = repository.findByMillIdOrderByFromPctAsc(millId).stream().map(MoistureRangeDto::from).toList();
        Long ownerId = millRepository.findById(millId).map(m -> m.getOwner() == null ? null : m.getOwner().getId()).orElse(null);
        List<MoistureRangeDto> global = getGlobal(ownerId);
        if (!mill.isEmpty()) return new MoistureTableDto(MoistureTableDto.Source.MILL, mill, mill, global);
        if (!global.isEmpty()) return new MoistureTableDto(MoistureTableDto.Source.GLOBAL, global, mill, global);
        return new MoistureTableDto(MoistureTableDto.Source.NONE, List.of(), mill, global);
    }

    /** Deduction % for a moisture reading at this mill (0 when no band matches). */
    @Transactional(readOnly = true)
    public BigDecimal deductionFor(Long millId, BigDecimal moisture) {
        return lookup(getForMill(millId).getRanges(), moisture);
    }

    /** Finds the band containing {@code moisture} (from inclusive, to exclusive). */
    public static BigDecimal lookup(List<MoistureRangeDto> ranges, BigDecimal moisture) {
        if (moisture == null) return BigDecimal.ZERO;
        return ranges.stream()
                .filter(r -> moisture.compareTo(r.getFromPct()) >= 0
                        && (r.getToPct() == null || moisture.compareTo(r.getToPct()) < 0))
                .map(MoistureRangeDto::getDeductionPct)
                .findFirst().orElse(BigDecimal.ZERO);
    }

    /** Replaces the company default table of this super admin; it applies to their mills without their own table. */
    @Transactional
    public List<MoistureRangeDto> replaceGlobal(Long ownerId, List<MoistureRangeDto> ranges) {
        List<MoistureRangeDto> clean = validate(ranges);
        repository.deleteCompanyDefault(ownerId);
        repository.flush();
        save(null, ownerId, clean);
        return getGlobal(ownerId);
    }

    /** Replaces the mill's own table. An empty list removes it, so the mill falls back to the global table. */
    @Transactional
    public MoistureTableDto replaceMill(Long millId, List<MoistureRangeDto> ranges) {
        Mill mill = millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found: " + millId));
        List<MoistureRangeDto> clean = validate(ranges);
        repository.deleteByMillId(millId);
        repository.flush();
        save(mill, null, clean);
        return getForMill(millId);
    }

    @Transactional
    public MoistureTableDto resetMill(Long millId) {
        repository.deleteByMillId(millId);
        return getForMill(millId);
    }

    private void save(Mill mill, Long ownerId, List<MoistureRangeDto> ranges) {
        for (MoistureRangeDto r : ranges) {
            MoistureDeduction d = new MoistureDeduction();
            d.setMill(mill);
            d.setOwnerId(ownerId);
            d.setFromPct(r.getFromPct());
            d.setToPct(r.getToPct());
            d.setDeductionPct(r.getDeductionPct());
            repository.save(d);
        }
    }

    /**
     * Checks every band (0-100, from < to, at most 2 decimals) and that bands don't overlap.
     * Returns the bands sorted by fromPct. Messages refer to rows in that sorted order.
     */
    static List<MoistureRangeDto> validate(List<MoistureRangeDto> input) {
        if (input == null) throw new IllegalArgumentException("Ranges are required");
        if (input.size() > MAX_RANGES) throw new IllegalArgumentException("At most " + MAX_RANGES + " ranges are allowed");
        for (MoistureRangeDto r : input) {
            if (r == null || r.getFromPct() == null) throw new IllegalArgumentException("Every range needs a 'from' moisture %");
            if (r.getDeductionPct() == null) throw new IllegalArgumentException("Every range needs a deduction %");
        }
        List<MoistureRangeDto> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparing(MoistureRangeDto::getFromPct));

        for (int i = 0; i < sorted.size(); i++) {
            MoistureRangeDto r = sorted.get(i);
            String row = "Range " + r.getFromPct().stripTrailingZeros().toPlainString() + "%: ";
            checkPercent(r.getFromPct(), row + "'from'");
            if (r.getToPct() != null) {
                checkPercent(r.getToPct(), row + "'to'");
                if (r.getToPct().compareTo(r.getFromPct()) <= 0) {
                    throw new IllegalArgumentException(row + "'to' must be greater than 'from'");
                }
            }
            checkPercent(r.getDeductionPct(), row + "deduction");
            if (i > 0) {
                MoistureRangeDto prev = sorted.get(i - 1);
                if (prev.getToPct() == null || prev.getToPct().compareTo(r.getFromPct()) > 0) {
                    throw new IllegalArgumentException(row + "overlaps the previous range");
                }
            }
        }
        return sorted;
    }

    private static void checkPercent(BigDecimal v, String label) {
        if (v.compareTo(BigDecimal.ZERO) < 0 || v.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException(label + " must be between 0 and 100");
        }
        if (v.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(label + " can have at most 2 decimals");
        }
    }
}
