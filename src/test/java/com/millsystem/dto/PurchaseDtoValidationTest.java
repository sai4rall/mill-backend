package com.millsystem.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private java.util.List<String> moistureErrors(BigDecimal moisture) {
        PurchaseDto dto = new PurchaseDto();
        dto.setMoisturePct(moisture);
        return validator.validateProperty(dto, "moisturePct").stream().map(v -> v.getMessage()).toList();
    }

    @Test
    void moistureMayBeOmittedAtRequestLevel_theServiceRequiresItOnlyWhenTheFeatureIsOn() {
        assertThat(moistureErrors(null)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "14.5", "16.25", "100"})
    void acceptsValidMoisture(String v) {
        assertThat(moistureErrors(new BigDecimal(v))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "100.01", "250"})
    void rejectsOutOfRange(String v) {
        assertThat(moistureErrors(new BigDecimal(v))).contains("Moisture % must be between 0 and 100");
    }

    @Test
    void rejectsMoreThanTwoDecimals() {
        assertThat(moistureErrors(new BigDecimal("16.255"))).contains("Moisture % can have at most 2 decimals");
    }
}
