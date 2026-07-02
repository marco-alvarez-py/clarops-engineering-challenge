package com.clara.challenge.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.challenge.dtos.TraceStatusResponse;
import com.clara.challenge.entities.Event;
import com.clara.challenge.entities.TraceState;
import com.clara.challenge.enums.EventResult;
import com.clara.challenge.enums.TraceStatus;
import com.clara.challenge.exceptions.TraceNotFoundException;
import com.clara.challenge.repositories.TraceStateRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Validates {@link TraceStateService}: the lazy TTL-expiration rule applied on read, and the
 * bookkeeping performed when persisting a new state (new trace vs. existing trace, and the
 * per-transition fields copied from {@link TraceStateTransitionResolver}).
 */
@ExtendWith(MockitoExtension.class)
class TraceStateServiceTest {

  @Mock private TraceStateRepository traceStateRepository;

  private TraceStateService traceStateService;

  @BeforeEach
  void setUp() {
    traceStateService = new TraceStateService(traceStateRepository);
  }

  private static TraceState waitingState(Instant nextExpectedBefore) {
    TraceState state = new TraceState();
    state.setTraceId("trace-1");
    state.setStatus(TraceStatus.WAITING_OTHER_EVENT);
    state.setNextExpectedEvent("payment.captured");
    state.setNextExpectedBefore(nextExpectedBefore);
    state.setEventsReceived(1);
    return state;
  }

  private static Event newEvent(
      String traceId, boolean finalEvent, String nextExpectedEvent, Integer ttlSeconds) {
    Event event = new Event();
    event.setId(UUID.randomUUID());
    event.setTraceId(traceId);
    event.setEventName("order.created");
    event.setResult(EventResult.SUCCESS);
    event.setOccurredAt(Instant.parse("2026-07-02T10:00:00Z"));
    event.setFinalEvent(finalEvent);
    event.setNextExpectedEvent(nextExpectedEvent);
    event.setNextEventTtlSeconds(ttlSeconds);
    return event;
  }

  @Test
  void getStatus_traceNotFound_throwsTraceNotFoundException() {
    when(traceStateRepository.findById("missing")).thenReturn(Optional.empty());

    assertThrows(TraceNotFoundException.class, () -> traceStateService.getStatus("missing"));
  }

  @Test
  void getStatus_waitingOtherEventWithDeadlineInPast_returnsTtlExpiredStatus() {
    TraceState state = waitingState(Instant.parse("2026-07-01T00:00:00Z"));
    when(traceStateRepository.findById("trace-1")).thenReturn(Optional.of(state));
    when(traceStateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TraceStatusResponse response = traceStateService.getStatus("trace-1");

    assertThat(response.status()).isEqualTo(TraceStatus.TTL_EXPIRED_FOR_EVENT);
  }

  @Test
  void getStatus_waitingOtherEventWithDeadlineInPast_persistsExpiredStatus() {
    TraceState state = waitingState(Instant.parse("2026-07-01T00:00:00Z"));
    when(traceStateRepository.findById("trace-1")).thenReturn(Optional.of(state));
    when(traceStateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    traceStateService.getStatus("trace-1");

    verify(traceStateRepository).save(state);
    assertThat(state.getStatus()).isEqualTo(TraceStatus.TTL_EXPIRED_FOR_EVENT);
  }

  @Test
  void getStatus_waitingOtherEventWithDeadlineInFuture_returnsWaitingOtherEventStatus() {
    TraceState state = waitingState(Instant.now().plusSeconds(3600));
    when(traceStateRepository.findById("trace-1")).thenReturn(Optional.of(state));

    TraceStatusResponse response = traceStateService.getStatus("trace-1");

    assertThat(response.status()).isEqualTo(TraceStatus.WAITING_OTHER_EVENT);
    verify(traceStateRepository, never()).save(any());
  }

  @Test
  void getStatus_completedTrace_isNeverReportedAsTtlExpired() {
    TraceState state = waitingState(Instant.parse("2026-07-01T00:00:00Z"));
    state.setStatus(TraceStatus.COMPLETED);
    when(traceStateRepository.findById("trace-1")).thenReturn(Optional.of(state));

    TraceStatusResponse response = traceStateService.getStatus("trace-1");

    assertThat(response.status()).isEqualTo(TraceStatus.COMPLETED);
    verify(traceStateRepository, never()).save(any());
  }

  @Test
  void getStatus_startedTraceWithNoDeadline_returnsStartedStatus() {
    TraceState state = waitingState(null);
    state.setStatus(TraceStatus.STARTED);
    state.setNextExpectedEvent(null);
    when(traceStateRepository.findById("trace-1")).thenReturn(Optional.of(state));

    TraceStatusResponse response = traceStateService.getStatus("trace-1");

    assertThat(response.status()).isEqualTo(TraceStatus.STARTED);
    verify(traceStateRepository, never()).save(any());
  }

  @Test
  void getStatus_alreadyFlaggedTtlExpired_doesNotSaveAgain() {
    TraceState state = waitingState(Instant.parse("2026-07-01T00:00:00Z"));
    state.setStatus(TraceStatus.TTL_EXPIRED_FOR_EVENT);
    when(traceStateRepository.findById("trace-1")).thenReturn(Optional.of(state));

    TraceStatusResponse response = traceStateService.getStatus("trace-1");

    assertThat(response.status()).isEqualTo(TraceStatus.TTL_EXPIRED_FOR_EVENT);
    verify(traceStateRepository, never()).save(any());
  }

  @Test
  void isTtlExpired_statusAlreadyTtlExpired_returnsTrue() {
    TraceState state = waitingState(null);
    state.setStatus(TraceStatus.TTL_EXPIRED_FOR_EVENT);

    assertThat(traceStateService.isTtlExpired(state)).isTrue();
  }

  @Test
  void isTtlExpired_waitingOtherEventWithDeadlineInPast_returnsTrue() {
    TraceState state = waitingState(Instant.parse("2026-07-01T00:00:00Z"));

    assertThat(traceStateService.isTtlExpired(state)).isTrue();
  }

  @Test
  void isTtlExpired_waitingOtherEventWithDeadlineInFuture_returnsFalse() {
    TraceState state = waitingState(Instant.now().plusSeconds(3600));

    assertThat(traceStateService.isTtlExpired(state)).isFalse();
  }

  @Test
  void isTtlExpired_statusStarted_returnsFalse() {
    TraceState state = waitingState(Instant.parse("2026-07-01T00:00:00Z"));
    state.setStatus(TraceStatus.STARTED);

    assertThat(traceStateService.isTtlExpired(state)).isFalse();
  }

  @Test
  void isTtlExpired_statusCompleted_returnsFalse() {
    TraceState state = waitingState(Instant.parse("2026-07-01T00:00:00Z"));
    state.setStatus(TraceStatus.COMPLETED);

    assertThat(traceStateService.isTtlExpired(state)).isFalse();
  }

  @Test
  void markTtlExpired_statusWaitingOtherEvent_flipsToTtlExpiredAndPersists() {
    TraceState state = waitingState(Instant.parse("2026-07-01T00:00:00Z"));
    when(traceStateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    traceStateService.markTtlExpired(state);

    assertThat(state.getStatus()).isEqualTo(TraceStatus.TTL_EXPIRED_FOR_EVENT);
    verify(traceStateRepository).save(state);
  }

  @Test
  void markTtlExpired_statusAlreadyTtlExpired_doesNotPersistAgain() {
    TraceState state = waitingState(Instant.parse("2026-07-01T00:00:00Z"));
    state.setStatus(TraceStatus.TTL_EXPIRED_FOR_EVENT);

    traceStateService.markTtlExpired(state);

    verify(traceStateRepository, never()).save(any());
  }

  @Test
  void findById_traceExists_returnsTraceState() {
    TraceState state = waitingState(null);
    when(traceStateRepository.findById("trace-1")).thenReturn(Optional.of(state));

    TraceState result = traceStateService.findById("trace-1");

    assertThat(result).isSameAs(state);
  }

  @Test
  void findById_traceDoesNotExist_returnsNull() {
    when(traceStateRepository.findById("missing")).thenReturn(Optional.empty());

    TraceState result = traceStateService.findById("missing");

    assertThat(result).isNull();
  }

  @Test
  void save_noCurrentState_createsNewStateWithEventsReceivedOne() {
    Event event = newEvent("trace-new", false, null, null);
    when(traceStateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TraceState result = traceStateService.save(null, event);

    assertThat(result.getTraceId()).isEqualTo("trace-new");
    assertThat(result.getEventsReceived()).isEqualTo(1);
    assertThat(result.getCreatedAt()).isNotNull();
  }

  @Test
  void save_existingCurrentState_incrementsEventsReceivedCount() {
    TraceState currentState = waitingState(Instant.now().plusSeconds(3600));
    currentState.setEventsReceived(2);
    Event event = newEvent("trace-1", false, null, null);
    when(traceStateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TraceState result = traceStateService.save(currentState, event);

    assertThat(result.getEventsReceived()).isEqualTo(3);
  }

  @Test
  void save_finalEvent_setsStatusCompletedAndClearsNextExpectedFields() {
    Event event = newEvent("trace-1", true, null, null);
    when(traceStateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TraceState result = traceStateService.save(null, event);

    assertThat(result.getStatus()).isEqualTo(TraceStatus.COMPLETED);
    assertThat(result.getNextExpectedEvent()).isNull();
    assertThat(result.getNextExpectedBefore()).isNull();
  }

  @Test
  void save_eventWithNextExpectedEventAndTtl_setsStatusWaitingOtherEventWithDeadline() {
    Event event = newEvent("trace-1", false, "payment.captured", 3600);
    when(traceStateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TraceState result = traceStateService.save(null, event);

    assertThat(result.getStatus()).isEqualTo(TraceStatus.WAITING_OTHER_EVENT);
    assertThat(result.getNextExpectedEvent()).isEqualTo("payment.captured");
    assertThat(result.getNextExpectedBefore()).isEqualTo(Instant.parse("2026-07-02T11:00:00Z"));
  }

  @Test
  void save_eventWithoutNextExpectedEventOrTtl_setsStatusStarted() {
    Event event = newEvent("trace-1", false, null, null);
    when(traceStateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TraceState result = traceStateService.save(null, event);

    assertThat(result.getStatus()).isEqualTo(TraceStatus.STARTED);
  }
}
