package com.clara.challenge.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.clara.challenge.entities.Event;
import com.clara.challenge.enums.TraceStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Validates the pure state-transition rules documented on {@link TraceStateTransitionResolver}: a
 * final event always completes the trace, an event carrying both {@code nextExpectedEvent} and
 * {@code nextEventTtlSeconds} puts the trace into a waiting state with a computed deadline, and any
 * other event simply starts/continues the trace.
 */
class TraceStateTransitionResolverTest {

  private static Event newEvent(boolean finalEvent, String nextExpectedEvent, Integer ttlSeconds) {
    Event event = new Event();
    event.setId(UUID.randomUUID());
    event.setOccurredAt(Instant.parse("2026-07-02T10:00:00Z"));
    event.setFinalEvent(finalEvent);
    event.setNextExpectedEvent(nextExpectedEvent);
    event.setNextEventTtlSeconds(ttlSeconds);
    return event;
  }

  @Test
  void resolve_finalEventTrue_returnsCompletedStatus() {
    Event event = newEvent(true, null, null);

    TraceStateTransitionResult result = TraceStateTransitionResolver.resolve(event);

    assertThat(result.status()).isEqualTo(TraceStatus.COMPLETED);
  }

  @Test
  void resolve_finalEventTrueWithNextExpectedEventSet_completedTakesPriorityOverWaiting() {
    Event event = newEvent(true, "payment.captured", 3600);

    TraceStateTransitionResult result = TraceStateTransitionResolver.resolve(event);

    assertThat(result.status()).isEqualTo(TraceStatus.COMPLETED);
    assertThat(result.nextExpectedEvent()).isNull();
    assertThat(result.nextExpectedBefore()).isNull();
  }

  @Test
  void resolve_nextExpectedEventAndTtlProvided_returnsWaitingOtherEventStatus() {
    Event event = newEvent(false, "payment.captured", 3600);

    TraceStateTransitionResult result = TraceStateTransitionResolver.resolve(event);

    assertThat(result.status()).isEqualTo(TraceStatus.WAITING_OTHER_EVENT);
    assertThat(result.nextExpectedEvent()).isEqualTo("payment.captured");
  }

  @Test
  void resolve_nextExpectedEventAndTtlProvided_calculatesDeadlineAsOccurredAtPlusTtlSeconds() {
    Event event = newEvent(false, "payment.captured", 3600);

    TraceStateTransitionResult result = TraceStateTransitionResolver.resolve(event);

    assertThat(result.nextExpectedBefore()).isEqualTo(Instant.parse("2026-07-02T11:00:00Z"));
  }

  @Test
  void resolve_noNextExpectedEventAndNoTtl_returnsStartedStatus() {
    Event event = newEvent(false, null, null);

    TraceStateTransitionResult result = TraceStateTransitionResolver.resolve(event);

    assertThat(result.status()).isEqualTo(TraceStatus.STARTED);
    assertThat(result.nextExpectedEvent()).isNull();
    assertThat(result.nextExpectedBefore()).isNull();
  }
}
