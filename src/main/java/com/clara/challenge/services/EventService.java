package com.clara.challenge.services;

import com.clara.challenge.dtos.EventRequest;
import com.clara.challenge.dtos.EventResponse;
import com.clara.challenge.entities.Event;
import com.clara.challenge.entities.TraceState;
import com.clara.challenge.enums.TraceStatus;
import com.clara.challenge.exceptions.DuplicateEventException;
import com.clara.challenge.exceptions.TraceAlreadyCompletedException;
import com.clara.challenge.exceptions.UnexpectedEventException;
import com.clara.challenge.repositories.EventRepository;
import com.clara.challenge.repositories.TraceStateRepository;
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

  @Transactional
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
      if (!expected.equals(request.eventName())) {
        throw new UnexpectedEventException(request.traceId(), expected, request.eventName());
      }
    }

    Event event = buildEvent(request);
    eventRepository.save(event);

    TraceState newState = traceStateService.save(currentState, event);

    return new EventResponse(event.getEventId(), event.getTraceId(), newState.getStatus());
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
