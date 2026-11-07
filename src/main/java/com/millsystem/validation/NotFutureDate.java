package com.millsystem.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * The date must not be after today in the mill's time zone ({@link com.millsystem.config.AppTime}), and not before 2000.
 * The UIs enforce the same rule in their date pickers.
 */
@Documented
@Constraint(validatedBy = NotFutureDateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotFutureDate {
    String message() default "Date cannot be in the future";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
