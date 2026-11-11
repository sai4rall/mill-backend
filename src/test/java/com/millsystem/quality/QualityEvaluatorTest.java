package com.millsystem.quality;

import com.millsystem.dto.GradeLimitsDto;
import com.millsystem.dto.QualityStandardDto;
import com.millsystem.entity.QualityGrade;
import com.millsystem.entity.QualityTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.millsystem.entity.QualityGrade.*;
import static org.assertj.core.api.Assertions.assertThat;

class QualityEvaluatorTest {

    private static final QualityStandardDto RICE = QualityDefaults.forProduct(QualityTest.Product.RICE);

    private static QualityTest rice(String moisture, String broken) {
        QualityTest t = new QualityTest();
        t.setProduct(QualityTest.Product.RICE);
        t.setMoisturePct(new BigDecimal(moisture));
        t.setBrokenPct(broken == null ? null : new BigDecimal(broken));
        return t;
    }

    private static QualityGrade grade(QualityTest t, QualityStandardDto std) { return QualityEvaluator.evaluate(t, std).grade(); }

    @Test
    void theWorstParameterDecidesTheGrade() {
        assertThat(grade(rice("12", "4"), RICE)).isEqualTo(A_PLUS);
        assertThat(grade(rice("12", "5"), RICE)).isEqualTo(A_PLUS);   // exactly at the limit
        assertThat(grade(rice("12", "9"), RICE)).isEqualTo(A);
        assertThat(grade(rice("13.5", "4"), RICE)).isEqualTo(A);      // moisture pulls it down
        assertThat(grade(rice("12", "14"), RICE)).isEqualTo(B_PLUS);
        assertThat(grade(rice("14", "18"), RICE)).isEqualTo(B);
        QualityTest t = rice("12", "4");
        t.setChalkyPct(new BigDecimal("4.5"));
        assertThat(grade(t, RICE)).isEqualTo(C);                       // chalky alone
    }

    @Test
    void failsBelowGradeCWithReasonsAndNoGrade() {
        QualityTest t = rice("15.5", "30");
        t.setForeignMatterPct(new BigDecimal("0.4"));
        var out = QualityEvaluator.evaluate(t, RICE);
        assertThat(out.result()).isEqualTo(QualityTest.Result.FAIL);
        assertThat(out.grade()).isNull();
        assertThat(out.failures()).containsExactly("Moisture 15.50% is above the limit 14.00%", "Broken 30.00% is above the limit 25.00%");
    }

    @Test
    void aBlankLimitAtAHigherGradeStillRespectsTheLowerGrades() {
        // Only C limits moisture; A+ sets head rice only. A sample with 15% moisture must fail, not get A+.
        QualityStandardDto std = new QualityStandardDto(QualityTest.Product.RICE, true, List.of(
                GradeLimitsDto.of(A_PLUS, null, null, null, null, null, "70"),
                GradeLimitsDto.of(A, null, null, null, null, null, "65"),
                GradeLimitsDto.of(B_PLUS, null, null, null, null, null, null),
                GradeLimitsDto.of(B, null, null, null, null, null, null),
                GradeLimitsDto.of(C, "14", null, null, null, null, null)), null, null);
        QualityTest t = rice("15", null);
        t.setHeadRicePct(new BigDecimal("75"));
        assertThat(QualityEvaluator.evaluate(t, std).result()).isEqualTo(QualityTest.Result.FAIL);
        t.setMoisturePct(new BigDecimal("13"));
        assertThat(grade(t, std)).isEqualTo(A_PLUS);
        t.setHeadRicePct(new BigDecimal("66"));                         // head rice is a minimum
        assertThat(grade(t, std)).isEqualTo(A);
        t.setHeadRicePct(null);                                         // no reading: not checked
        assertThat(grade(t, std)).isEqualTo(A_PLUS);
    }
}
