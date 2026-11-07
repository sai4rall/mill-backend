package com.millsystem.validation;

import com.millsystem.config.AppTime;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

/**
 * Rejects dates after today in the business time zone (see {@link AppTime}) and implausibly old dates
 * (before {@link #EARLIEST}), which are almost always typos.
 */
public class NotFutureDateValidator implements ConstraintValidator<NotFutureDate, LocalDate> {

    static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext ctx) {
        if (value == null) return true;
        if (value.isBefore(EARLIEST)) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("Date cannot be before " + EARLIEST).addConstraintViolation();
            return false;
        }
        return !value.isAfter(AppTime.today());
    }
}
