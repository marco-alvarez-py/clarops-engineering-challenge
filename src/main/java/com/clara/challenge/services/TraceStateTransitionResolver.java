package com.clara.challenge.services;

import com.clara.challenge.entities.Event;
import com.clara.challenge.enums.TraceStatus;
import java.time.Instant;

/**
 * Pure state-transition logic, kept free of Spring/JPA so it can be unit tested by instantiating
 * {@link Event} directly, without a Spring context.
 *
 * <p>The resulting state is derived solely from the incoming event's own fields (finalEvent /
 * nextExpectedEvent / nextEventTtlSeconds), not from matching eventName against a previously
 * expected event.
 */
public final class TraceStateTransitionResolver {

  private TraceStateTransitionResolver() {}

  public static TraceStateTransitionResult resolve(Event event) {
    if (event.isFinalEvent()) {
      return new TraceStateTransitionResult(TraceStatus.COMPLETED, null, null);
    }

    if (event.getNextExpectedEvent() != null && event.getNextEventTtlSeconds() != null) {
      Instant deadline = event.getOccurredAt().plusSeconds(event.getNextEventTtlSeconds());
      return new TraceStateTransitionResult(
          TraceStatus.WAITING_OTHER_EVENT, event.getNextExpectedEvent(), deadline);
    }

    return new TraceStateTransitionResult(TraceStatus.STARTED, null, null);
  }
}
