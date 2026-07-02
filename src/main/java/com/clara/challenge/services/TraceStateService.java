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

    if (isTtlExpired(state)) {
      markTtlExpired(state);
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

  /**
   * A trace is expired either because it was already flagged as such on a previous read/ingest, or
   * because it is still waiting for an event whose deadline has now passed.
   */
  public boolean isTtlExpired(TraceState state) {
    return state.getStatus() == TraceStatus.TTL_EXPIRED_FOR_EVENT
        || (state.getStatus() == TraceStatus.WAITING_OTHER_EVENT
            && state.getNextExpectedBefore() != null
            && Instant.now().isAfter(state.getNextExpectedBefore()));
  }

  /**
   * TTL expiration is not a terminal state: it only marks that the previously expected event missed
   * its deadline. Persists the transition once, so repeated calls are a no-op.
   */
  public void markTtlExpired(TraceState state) {
    if (state.getStatus() != TraceStatus.TTL_EXPIRED_FOR_EVENT) {
      state.setStatus(TraceStatus.TTL_EXPIRED_FOR_EVENT);
      state.setUpdatedAt(Instant.now());
      traceStateRepository.save(state);
    }
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
