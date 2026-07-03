package com.clara.challenge.services;

import com.clara.challenge.dtos.EventRequest;
import com.clara.challenge.dtos.EventResponse;
import com.clara.challenge.entities.Event;
import com.clara.challenge.entities.TraceState;
import com.clara.challenge.enums.TraceStatus;
import com.clara.challenge.exceptions.DuplicateEventException;
import com.clara.challenge.exceptions.TraceAlreadyCompletedException;
import com.clara.challenge.exceptions.TtlExpiredException;
import com.clara.challenge.exceptions.UnexpectedEventException;
import com.clara.challenge.repositories.EventRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class EventService {

  private final EventRepository eventRepository;
  private final TraceStateService traceStateService;

  /**
   * TTL expiration is not terminal: a late arrival of the expected event is rejected (and eagerly
   * flips the trace to {@code TTL_EXPIRED_FOR_EVENT} if it wasn't already), but a different event
   * arriving after the deadline is accepted and the flow simply continues from it. The eager
   * expiration write must survive the {@link TtlExpiredException} rejection, hence {@code
   * noRollbackFor}.
   */
  @Transactional(noRollbackFor = TtlExpiredException.class)
  public EventResponse ingest(EventRequest request) {
    if (eventRepository.existsByEventId(request.eventId())) {
      throw new DuplicateEventException(request.eventId());
    }

    TraceState currentState = traceStateService.findById(request.traceId());
    if (currentState != null && currentState.getStatus() == TraceStatus.COMPLETED) {
      throw new TraceAlreadyCompletedException(request.traceId());
    }

    if (currentState != null && currentState.getNextExpectedEvent() != null) {
      String expected = currentState.getNextExpectedEvent();
      boolean nameMatches = expected.equals(request.eventName());
      boolean ttlExpired = traceStateService.isTtlExpired(currentState);

      if (nameMatches && ttlExpired) {
        traceStateService.markTtlExpired(currentState);
        throw new TtlExpiredException(request.traceId());
      }
      if (!nameMatches && !ttlExpired) {
        throw new UnexpectedEventException(request.traceId(), expected, request.eventName());
      }
      // nameMatches && !ttlExpired -> accepted, on time.
      // !nameMatches && ttlExpired -> accepted, the flow continues with this event instead.
    }

    Event event = buildEvent(request);
    eventRepository.save(event);

    traceStateService.save(currentState, event);

    return toResponse(event);
  }

  private EventResponse toResponse(Event event) {
    return new EventResponse(
        event.getEventId(),
        event.getTraceId(),
        event.getEventName(),
        event.getResult(),
        event.getOccurredAt(),
        event.getReceivedAt(),
        event.getNextExpectedEvent(),
        event.getNextEventTtlSeconds(),
        event.isFinalEvent());
  }

  private Event buildEvent(EventRequest request) {
    Event event = new Event();
    event.setId(UUID.randomUUID());
    event.setEventId(request.eventId());
    event.setTraceId(request.traceId());
    event.setEventName(request.eventName());
    event.setResult(request.result());
    event.setOccurredAt(request.occurredAt());
    event.setReceivedAt(Instant.now());
    event.setNextExpectedEvent(request.nextExpectedEvent());
    event.setNextEventTtlSeconds(request.nextEventTtlSeconds());
    event.setFinalEvent(Boolean.TRUE.equals(request.finalEvent()));
    event.setMetadata(toJson(request.metadata()));
    return event;
  }

  private String toJson(JsonNode metadata) {
    return metadata == null || metadata.isNull() ? null : metadata.toString();
  }
}
