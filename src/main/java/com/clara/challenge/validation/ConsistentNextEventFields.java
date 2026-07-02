package com.clara.challenge.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ensures {@code nextExpectedEvent} and {@code nextEventTtlSeconds} are either both present or both
 * absent, since a trace can only enter {@code WAITING_OTHER_EVENT} when both are known.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConsistentNextEventFieldsValidator.class)
@Documented
public @interface ConsistentNextEventFields {

  String message() default
      "nextExpectedEvent and nextEventTtlSeconds must both be provided or both be omitted";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
