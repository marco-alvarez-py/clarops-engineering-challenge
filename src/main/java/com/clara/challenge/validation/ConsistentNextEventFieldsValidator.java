package com.clara.challenge.validation;

import com.clara.challenge.dtos.EventRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConsistentNextEventFieldsValidator
    implements ConstraintValidator<ConsistentNextEventFields, EventRequest> {

  @Override
  public boolean isValid(EventRequest request, ConstraintValidatorContext context) {
    if (request == null) {
      return true;
    }
    boolean hasNextExpectedEvent = request.nextExpectedEvent() != null;
    boolean hasNextEventTtlSeconds = request.nextEventTtlSeconds() != null;
    if (hasNextExpectedEvent == hasNextEventTtlSeconds) {
      return true;
    }

    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
        .addPropertyNode(hasNextExpectedEvent ? "nextEventTtlSeconds" : "nextExpectedEvent")
        .addConstraintViolation();
    return false;
  }
}
