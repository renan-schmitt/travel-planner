package com.travelplanner.itinerary.api;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

/** Saída igual ou posterior à chegada; o erro sai no campo {@code departureDate}. */
@Documented
@Constraint(validatedBy = StopDates.Validator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface StopDates {

  String message() default "a saída deve ser igual ou posterior à chegada";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  interface Bounded {
    LocalDate arrivalDate();

    LocalDate departureDate();
  }

  class Validator implements ConstraintValidator<StopDates, Bounded> {

    @Override
    public boolean isValid(Bounded value, ConstraintValidatorContext context) {
      if (value == null
          || value.arrivalDate() == null
          || value.departureDate() == null
          || !value.departureDate().isBefore(value.arrivalDate())) {
        return true;
      }
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
          .addPropertyNode("departureDate")
          .addConstraintViolation();
      return false;
    }
  }
}
