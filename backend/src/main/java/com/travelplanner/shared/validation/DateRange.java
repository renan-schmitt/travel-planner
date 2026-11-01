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
import java.time.LocalDate;

/**
 * Data de fim igual ou posterior à de início, quando as duas existem. Aplicada no tipo (um DTO que
 * implementa {@link Bounded}); o erro sai no campo {@code endDate}.
 */
@Documented
@Constraint(validatedBy = DateRange.Validator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DateRange {

  String message() default "a data de fim deve ser igual ou posterior à de início";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /** DTO com um intervalo de datas opcional. */
  interface Bounded {
    LocalDate startDate();

    LocalDate endDate();
  }

  class Validator implements ConstraintValidator<DateRange, Bounded> {

    @Override
    public boolean isValid(Bounded value, ConstraintValidatorContext context) {
      if (value == null
          || value.startDate() == null
          || value.endDate() == null
          || !value.endDate().isBefore(value.startDate())) {
        return true;
      }
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
          .addPropertyNode("endDate")
          .addConstraintViolation();
      return false;
    }
  }
}
