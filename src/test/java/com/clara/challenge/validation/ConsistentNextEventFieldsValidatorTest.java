package com.clara.challenge.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.challenge.dtos.EventRequest;
import com.clara.challenge.enums.EventResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ConsistentNextEventFieldsValidatorTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  @Test
  void bothFieldsPresent_isValid() {
    EventRequest request =
        new EventRequest(
            "evt-1",
            "trace-1",
            "order.created",
            EventResult.SUCCESS,
            Instant.now(),
            "payment.captured",
            3600,
            false,
            null);

    Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);

    assertThat(violations).isEmpty();
  }

  @Test
  void bothFieldsAbsent_isValid() {
    EventRequest request =
        new EventRequest(
            "evt-1",
            "trace-1",
            "order.created",
            EventResult.SUCCESS,
            Instant.now(),
            null,
            null,
            true,
            null);

    Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);

    assertThat(violations).isEmpty();
  }

  @Test
  void onlyNextExpectedEventPresent_isInvalid() {
    EventRequest request =
        new EventRequest(
            "evt-1",
            "trace-1",
            "order.created",
            EventResult.SUCCESS,
            Instant.now(),
            "payment.captured",
            null,
            false,
            null);

    Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);

    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getPropertyPath().toString())
        .isEqualTo("nextEventTtlSeconds");
  }

  @Test
  void onlyNextEventTtlSecondsPresent_isInvalid() {
    EventRequest request =
        new EventRequest(
            "evt-1",
            "trace-1",
            "order.created",
            EventResult.SUCCESS,
            Instant.now(),
            null,
            3600,
            false,
            null);

    Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);

    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getPropertyPath().toString())
        .isEqualTo("nextExpectedEvent");
  }
}
