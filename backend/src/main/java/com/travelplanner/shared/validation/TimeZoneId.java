package com.travelplanner.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.DateTimeException;
import java.time.ZoneId;

/** Identificador de fuso IANA válido (ex.: {@code America/Sao_Paulo}). Nulo é aceito. */
@Documented
@Constraint(validatedBy = TimeZoneId.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface TimeZoneId {

  String message() default "fuso horário inválido";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validator implements ConstraintValidator<TimeZoneId, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
      if (value == null) {
        return true;
      }
      try {
        ZoneId.of(value);
        return true;
      } catch (DateTimeException e) {
        return false;
      }
    }
  }
}
