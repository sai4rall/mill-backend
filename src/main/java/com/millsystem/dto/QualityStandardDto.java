package com.millsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.millsystem.entity.QualityGrade;
import com.millsystem.entity.QualityTest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A company's quality standard for one product: limits for each grade, A+ (best) to C (lowest pass).
 * A sample earns the best grade whose limits (together with every lower grade's) it meets on every parameter.
 */
public class QualityStandardDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private QualityTest.Product product;
    /** False while the company is still on the built-in default grades. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private boolean configured;

    @NotNull(message = "Grades are required")
    @Size(min = 5, max = 5, message = "Give limits for all five grades: A+, A, B+, B and C")
    private List<@Valid @NotNull GradeLimitsDto> grades;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private String updatedBy;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDateTime updatedAt;

    public QualityStandardDto() {}

    public QualityStandardDto(QualityTest.Product product, boolean configured, List<GradeLimitsDto> grades, String updatedBy, LocalDateTime updatedAt) {
        this.product = product; this.configured = configured; this.grades = grades; this.updatedBy = updatedBy; this.updatedAt = updatedAt;
    }

    /** The limits of one grade (null if missing). */
    @JsonIgnore
    public GradeLimitsDto limitsFor(QualityGrade grade) {
        return grades == null ? null : grades.stream().filter(g -> g.getGrade() == grade).findFirst().orElse(null);
    }

    public QualityTest.Product getProduct() { return product; }
    public void setProduct(QualityTest.Product product) { this.product = product; }
    public boolean isConfigured() { return configured; }
    public List<GradeLimitsDto> getGrades() { return grades; }
    public void setGrades(List<GradeLimitsDto> grades) { this.grades = grades; }
    public String getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
