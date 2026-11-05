package com.millsystem.dto;

import java.util.List;

/**
 * A mill's moisture deduction table. {@code source} says which table is in effect:
 * MILL (the mill's own ranges), GLOBAL (super admin defaults) or NONE (no ranges → no deduction).
 */
public class MoistureTableDto {
    public enum Source { MILL, GLOBAL, NONE }

    private final Source source;
    private final List<MoistureRangeDto> ranges;
    private final List<MoistureRangeDto> millRanges;
    private final List<MoistureRangeDto> globalRanges;

    public MoistureTableDto(Source source, List<MoistureRangeDto> ranges,
                            List<MoistureRangeDto> millRanges, List<MoistureRangeDto> globalRanges) {
        this.source = source; this.ranges = ranges; this.millRanges = millRanges; this.globalRanges = globalRanges;
    }

    public Source getSource() { return source; }
    public List<MoistureRangeDto> getRanges() { return ranges; }
    public List<MoistureRangeDto> getMillRanges() { return millRanges; }
    public List<MoistureRangeDto> getGlobalRanges() { return globalRanges; }
}
