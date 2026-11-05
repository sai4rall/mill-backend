package com.millsystem.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SuperAdminDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean usernameValid(String username) {
        SuperAdminDto dto = new SuperAdminDto();
        dto.setUsername(username);
        return validator.validateProperty(dto, "username").isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ops", "ops_admin", "ops.admin-2", " ops_admin "})
    void acceptsValidUsernames(String u) { assertThat(usernameValid(u)).isTrue(); }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ab", "ops admin", "ops@admin", "<script>"})
    void rejectsInvalidUsernames(String u) { assertThat(usernameValid(u)).isFalse(); }

    @Test
    void rejectsUsernameLongerThan50() { assertThat(usernameValid("a".repeat(51))).isFalse(); }

    @Test
    void fullNameIsTrimmedAndLimited() {
        SuperAdminDto dto = new SuperAdminDto();
        dto.setUsername("ops");
        dto.setFullName("  Ops Admin ");
        assertThat(dto.getFullName()).isEqualTo("Ops Admin");
        dto.setFullName("x".repeat(101));
        assertThat(validator.validateProperty(dto, "fullName")).isNotEmpty();
    }
}
