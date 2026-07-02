package com.clara.challenge.services;

import com.clara.challenge.dtos.TraceStatusResponse;
import com.clara.challenge.entities.Event;
import com.clara.challenge.entities.TraceState;
import com.clara.challenge.enums.TraceStatus;
import com.clara.challenge.exceptions.TraceNotFoundException;
import com.clara.challenge.repositories.TraceStateRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TraceStateService {

  private final TraceStateRepository traceStateRepository;

  @Transactional
  public TraceStatusResponse getStatus(String traceId) {
    TraceState state =
        traceStateRepository
            .findById(traceId)
            .orElseThrow(() -> new TraceNotFoundException(traceId));

    if (hasTtlExpired(state)) {
      state.setStatus(TraceStatus.TTL_EXPIRED_FOR_EVENT);
      traceStateRepository.save(state);
    }

    return new TraceStatusResponse(
        state.getTraceId(),
        state.getStatus(),
        state.getLastEventName(),
        state.getLastEventResult(),
        state.getNextExpectedEvent(),
        state.getNextExpectedBefore(),
        state.getEventsReceived());
  }

  public TraceState findById(String traceId) {
    return traceStateRepository.findById(traceId).orElse(null);
  }

  public TraceState save(TraceState currentState, Event event) {
    TraceStateTransitionResult transition = TraceStateTransitionResolver.resolve(event);
    TraceState newState = applyTransition(currentState, event, transition);
    return traceStateRepository.save(newState);
  }

  private boolean hasTtlExpired(TraceState state) {
    return state.getStatus() == TraceStatus.WAITING_OTHER_EVENT
            && state.getNextExpectedBefore() != null
            && Instant.now().isAfter(state.getNextExpectedBefore());
  }

  private TraceState applyTransition(
          TraceState currentState, Event event, TraceStateTransitionResult transition) {
    Instant now = Instant.now();
    TraceState state = currentState != null ? currentState : new TraceState();

    if (currentState == null) {
      state.setTraceId(event.getTraceId());
      state.setCreatedAt(now);
    } else {
      state.setEventsReceived(currentState.getEventsReceived() + 1);
    }

    state.setStatus(transition.status());
    state.setLastEventId(event.getId());
    state.setLastEventName(event.getEventName());
    state.setLastEventResult(event.getResult());
    state.setNextExpectedEvent(transition.nextExpectedEvent());
    state.setNextExpectedBefore(transition.nextExpectedBefore());
    state.setUpdatedAt(now);
    return state;
  }
}
