package com.millsystem.dto;

import com.millsystem.entity.MillBatch;
import com.millsystem.entity.QualityGrade;
import com.millsystem.entity.QualityTest;
import com.millsystem.entity.Shift;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Quality history of one milling batch: how many tests, the latest result and grade, and average readings. */
public record BatchQualityDto(
        Long batchId, LocalDate batchDate, MillBatch.BatchStatus batchStatus, Shift shift, BigDecimal paddyInputQuintals,
        int testCount, int failCount, int certificateCount,
        QualityTest.Result latestResult, QualityGrade latestGrade, LocalDate latestTestDate, BigDecimal latestMoisturePct, BigDecimal latestBrokenPct,
        BigDecimal avgMoisturePct, BigDecimal avgBrokenPct) {
}
