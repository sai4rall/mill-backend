package com.millsystem.quality;

import com.millsystem.dto.GradeLimitsDto;
import com.millsystem.dto.QualityStandardDto;
import com.millsystem.entity.QualityTest;

import java.util.List;

import static com.millsystem.entity.QualityGrade.*;

/**
 * Built-in grades used until a super admin saves the company's own. Grade C is close to common FCI procurement norms
 * (paddy: moisture 17%; raw rice: moisture 14%, brokens 25%, foreign matter 0.5%, damaged/discoloured 3%, chalky 5%);
 * the higher grades tighten each limit step by step. They are starting points to adjust, not an official grading.
 */
public final class QualityDefaults {
    private QualityDefaults() {}

    public static QualityStandardDto forProduct(QualityTest.Product product) {
        //                       moisture  broken  foreign  damaged  chalky  head rice (min)
        List<GradeLimitsDto> grades = switch (product) {
            case PADDY -> List.of(
                    GradeLimitsDto.of(A_PLUS, "14",   null, "0.5",  "1",   null, null),
                    GradeLimitsDto.of(A,      "14.5", null, "0.75", "2",   null, null),
                    GradeLimitsDto.of(B_PLUS, "15",   null, "1",    "3",   null, null),
                    GradeLimitsDto.of(B,      "16",   null, "1.5",  "4",   null, null),
                    GradeLimitsDto.of(C,      "17",   null, "2",    "5",   null, null));
            case RICE -> List.of(
                    GradeLimitsDto.of(A_PLUS, "13",   "5",  "0.1",  "1",   "1", null),
                    GradeLimitsDto.of(A,      "13.5", "10", "0.25", "1.5", "2", null),
                    GradeLimitsDto.of(B_PLUS, "13.5", "15", "0.4",  "2",   "3", null),
                    GradeLimitsDto.of(B,      "14",   "20", "0.5",  "2.5", "4", null),
                    GradeLimitsDto.of(C,      "14",   "25", "0.5",  "3",   "5", null));
        };
        return new QualityStandardDto(product, false, grades, null, null);
    }
}
