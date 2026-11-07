package com.millsystem.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChangePasswordRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private List<String> newPasswordErrors(String pw) {
        ChangePasswordRequest r = new ChangePasswordRequest();
        r.setCurrentPassword("x");
        r.setNewPassword(pw);
        return validator.validateProperty(r, "newPassword").stream().map(v -> v.getMessage()).toList();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcdefg1", "Admin@123", "DevAdmin@123", "pässwört9x"})
    void acceptsValidPasswords(String pw) {
        assertThat(newPasswordErrors(pw)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void newPasswordIsRequired(String pw) {
        assertThat(newPasswordErrors(pw)).contains("New password is required");
    }

    @Test
    void lengthIs8To64() {
        assertThat(newPasswordErrors("abc12")).contains("New password must be 8-64 characters");
        assertThat(newPasswordErrors("a1".repeat(32))).isEmpty();
        assertThat(newPasswordErrors("a1".repeat(32) + "x")).contains("New password must be 8-64 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {"onlyletters", "12345678", "has space1"})
    void needsLetterAndDigitWithoutSpaces(String pw) {
        assertThat(newPasswordErrors(pw)).contains("New password needs at least one letter and one digit, and no spaces");
    }

    @Test
    void currentPasswordIsRequired() {
        ChangePasswordRequest r = new ChangePasswordRequest();
        r.setNewPassword("abcdefg1");
        assertThat(validator.validateProperty(r, "currentPassword")).isNotEmpty();
    }
}
