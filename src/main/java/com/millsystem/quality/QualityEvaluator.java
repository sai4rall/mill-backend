package com.millsystem.quality;

import com.millsystem.dto.GradeLimitsDto;
import com.millsystem.dto.QualityStandardDto;
import com.millsystem.entity.QualityGrade;
import com.millsystem.entity.QualityTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Grades a lab sample against a quality standard. A sample earns the best grade whose limits it meets on every
 * parameter; the limit for a grade also includes every lower grade's (a blank limit at A+ does not let a sample
 * past the C limit). Below C the sample fails. Parameters without a reading or without any limit are skipped.
 */
public final class QualityEvaluator {
    private QualityEvaluator() {}

    public record Outcome(QualityTest.Result result, QualityGrade grade, List<String> failures) {}

    public record Check(String key, String label, boolean minimum, Function<QualityTest, BigDecimal> reading, Function<GradeLimitsDto, BigDecimal> limit) {}

    public static final List<Check> CHECKS = List.of(
            new Check("moisturePct", "Moisture", false, QualityTest::getMoisturePct, GradeLimitsDto::getMaxMoisturePct),
            new Check("brokenPct", "Broken", false, QualityTest::getBrokenPct, GradeLimitsDto::getMaxBrokenPct),
            new Check("foreignMatterPct", "Foreign matter", false, QualityTest::getForeignMatterPct, GradeLimitsDto::getMaxForeignMatterPct),
            new Check("damagedPct", "Damaged / discoloured", false, QualityTest::getDamagedPct, GradeLimitsDto::getMaxDamagedPct),
            new Check("chalkyPct", "Chalky", false, QualityTest::getChalkyPct, GradeLimitsDto::getMaxChalkyPct),
            new Check("headRicePct", "Head rice", true, QualityTest::getHeadRicePct, GradeLimitsDto::getMinHeadRicePct));

    public static Outcome evaluate(QualityTest sample, QualityStandardDto standard) {
        List<String> failures = new ArrayList<>();
        for (Check c : CHECKS) {
            BigDecimal value = c.reading().apply(sample), limit = effectiveLimit(c, QualityGrade.C, standard);
            if (value != null && limit != null && !within(c, value, limit)) {
                failures.add(c.label() + " " + pct(value) + " is " + (c.minimum() ? "below the minimum " : "above the limit ") + pct(limit));
            }
        }
        if (!failures.isEmpty()) return new Outcome(QualityTest.Result.FAIL, null, failures);
        for (QualityGrade g : QualityGrade.values()) { // best first
            boolean meets = CHECKS.stream().allMatch(c -> {
                BigDecimal value = c.reading().apply(sample), limit = effectiveLimit(c, g, standard);
                return value == null || limit == null || within(c, value, limit);
            });
            if (meets) return new Outcome(QualityTest.Result.PASS, g, List.of());
        }
        return new Outcome(QualityTest.Result.PASS, QualityGrade.C, List.of()); // unreachable: C was checked above
    }

    /** The strictest limit of this grade and every lower grade (null when none of them sets one). */
    public static BigDecimal effectiveLimit(Check c, QualityGrade grade, QualityStandardDto standard) {
        BigDecimal strictest = null;
        for (QualityGrade g : QualityGrade.values()) {
            if (g.ordinal() < grade.ordinal()) continue;
            GradeLimitsDto limits = standard.limitsFor(g);
            BigDecimal v = limits == null ? null : c.limit().apply(limits);
            if (v != null && (strictest == null || (c.minimum() ? v.compareTo(strictest) > 0 : v.compareTo(strictest) < 0))) strictest = v;
        }
        return strictest;
    }

    private static boolean within(Check c, BigDecimal value, BigDecimal limit) {
        int cmp = value.compareTo(limit);
        return c.minimum() ? cmp >= 0 : cmp <= 0;
    }

    private static String pct(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%"; }
}
