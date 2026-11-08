package com.example.peacockxt.DTO;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = {})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Pattern(regexp = "^[1-9]\\d{16,18}$", message = "Invalid Snowflake ID format")
public @interface ValidSnowflake {
    String message() default "Invalid Snowflake ID format. Must be a 17-19 digit number.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}