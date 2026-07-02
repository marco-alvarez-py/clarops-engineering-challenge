package com.clara.challenge.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.challenge.dtos.EventRequest;
import com.clara.challenge.dtos.EventResponse;
import com.clara.challenge.entities.Event;
import com.clara.challenge.entities.TraceState;
import com.clara.challenge.enums.EventResult;
import com.clara.challenge.enums.TraceStatus;
import com.clara.challenge.exceptions.DuplicateEventException;
import com.clara.challenge.exceptions.TraceAlreadyCompletedException;
import com.clara.challenge.exceptions.TtlExpiredException;
import com.clara.challenge.exceptions.UnexpectedEventException;
import com.clara.challenge.repositories.EventRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Validates the ingestion rules owned by {@link EventService}: duplicate event rejection, the
 * "completed traces are closed" rule, and the four-way combination of event-name matching x TTL
 * expiration for a trace that is waiting on a specific next event:
 *
 * <ul>
 *   <li>expected name, before deadline -&gt; accepted
 *   <li>expected name, after deadline -&gt; rejected, trace eagerly flipped to {@code
 *       TTL_EXPIRED_FOR_EVENT} if not already
 *   <li>different name, before deadline -&gt; rejected ({@link UnexpectedEventException})
 *   <li>different name, after deadline -&gt; accepted, the flow continues with the new event (TTL
 *       expiration is not a terminal state)
 * </ul>
 *
 * State-transition math itself is covered by {@link TraceStateTransitionResolverTest} and {@link
 * TraceStateServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

  @Mock private EventRepository eventRepository;
  @Mock private TraceStateService traceStateService;

  private EventService eventService;

  @BeforeEach
  void setUp() {
    eventService = new EventService(eventRepository, traceStateService);
  }

  private static EventRequest newRequest(
      String eventId, String traceId, String eventName, String nextExpectedEvent, Integer ttl, Boolean finalEvent) {
    return new EventRequest(
        eventId,
        traceId,
        eventName,
        EventResult.SUCCESS,
        Instant.parse("2026-07-02T10:00:00Z"),
        nextExpectedEvent,
        ttl,
        finalEvent,
        null);
  }

  private static TraceState createTraceState(TraceStatus status, String nextExpectedEvent) {
    TraceState state = new TraceState();
    state.setTraceId("trace-1");
    state.setStatus(status);
    state.setNextExpectedEvent(nextExpectedEvent);
    state.setEventsReceived(1);
    return state;
  }

  @Test
  void ingest_duplicateEventId_throwsDuplicateEventException() {
    EventRequest request = newRequest("evt-1", "trace-1", "order.created", null, null, Boolean.FALSE);
    when(eventRepository.existsByEventId("evt-1")).thenReturn(true);

    assertThrows(DuplicateEventException.class, () -> eventService.ingest(request));

    verify(traceStateService, never()).findById(any());
    verify(eventRepository, never()).save(any());
  }

  @Test
  void ingest_traceAlreadyCompleted_throwsTraceAlreadyCompletedException() {
    EventRequest request = newRequest("evt-2", "trace-1", "refund.issued", null, null, Boolean.FALSE);
    when(eventRepository.existsByEventId("evt-2")).thenReturn(false);
    when(traceStateService.findById("trace-1"))
        .thenReturn(createTraceState(TraceStatus.COMPLETED, null));

    assertThrows(TraceAlreadyCompletedException.class, () -> eventService.ingest(request));

    verify(eventRepository, never()).save(any());
  }

  @Test
  void ingest_differentNameBeforeDeadline_throwsUnexpectedEventException() {
    EventRequest request = newRequest("evt-3", "trace-1", "shipment.created", null, null, Boolean.FALSE);
    when(eventRepository.existsByEventId("evt-3")).thenReturn(false);
    TraceState currentState = createTraceState(TraceStatus.WAITING_OTHER_EVENT, "payment.captured");
    when(traceStateService.findById("trace-1")).thenReturn(currentState);
    when(traceStateService.isTtlExpired(currentState)).thenReturn(false);

    assertThrows(UnexpectedEventException.class, () -> eventService.ingest(request));

    verify(eventRepository, never()).save(any());
    verify(traceStateService, never()).markTtlExpired(any());
  }

  @Test
  void ingest_expectedNameBeforeDeadline_isAccepted() {
    EventRequest request = newRequest("evt-4", "trace-1", "payment.captured", null, null, Boolean.FALSE);
    when(eventRepository.existsByEventId("evt-4")).thenReturn(false);
    TraceState currentState = createTraceState(TraceStatus.WAITING_OTHER_EVENT, "payment.captured");
    when(traceStateService.findById("trace-1")).thenReturn(currentState);
    when(traceStateService.isTtlExpired(currentState)).thenReturn(false);
    TraceState newState = createTraceState(TraceStatus.STARTED, null);
    when(traceStateService.save(any(), any())).thenReturn(newState);

    EventResponse response = eventService.ingest(request);

    assertThat(response.eventId()).isEqualTo("evt-4");
    verify(eventRepository).save(any(Event.class));
  }

  @Test
  void ingest_noExistingTraceState_isAcceptedWithoutValidation() {
    EventRequest request = newRequest("evt-5", "trace-new", "order.created", null, null, Boolean.FALSE);
    when(eventRepository.existsByEventId("evt-5")).thenReturn(false);
    when(traceStateService.findById("trace-new")).thenReturn(null);
    TraceState newState = createTraceState(TraceStatus.STARTED, null);
    newState.setTraceId("trace-new");
    when(traceStateService.save(any(), any())).thenReturn(newState);

    EventResponse response = eventService.ingest(request);

    assertThat(response.traceId()).isEqualTo("trace-new");
    assertThat(response.status()).isEqualTo(TraceStatus.STARTED);
  }

  @Test
  void ingest_expectedNameAfterDeadline_throwsTtlExpiredExceptionAndMarksTraceExpired() {
    EventRequest request = newRequest("evt-6", "trace-1", "payment.captured", null, null, Boolean.FALSE);
    when(eventRepository.existsByEventId("evt-6")).thenReturn(false);
    TraceState currentState = createTraceState(TraceStatus.WAITING_OTHER_EVENT, "payment.captured");
    when(traceStateService.findById("trace-1")).thenReturn(currentState);
    when(traceStateService.isTtlExpired(currentState)).thenReturn(true);

    assertThrows(TtlExpiredException.class, () -> eventService.ingest(request));

    verify(traceStateService).markTtlExpired(currentState);
    verify(eventRepository, never()).save(any());
    verify(traceStateService, never()).save(any(), any());
  }

  @Test
  void ingest_expectedNameAfterDeadline_doesNotMarkExpiredAgainWhenAlreadyFlagged() {
    EventRequest request = newRequest("evt-6b", "trace-1", "payment.captured", null, null, Boolean.FALSE);
    when(eventRepository.existsByEventId("evt-6b")).thenReturn(false);
    TraceState currentState = createTraceState(TraceStatus.TTL_EXPIRED_FOR_EVENT, "payment.captured");
    when(traceStateService.findById("trace-1")).thenReturn(currentState);
    when(traceStateService.isTtlExpired(currentState)).thenReturn(true);

    assertThrows(TtlExpiredException.class, () -> eventService.ingest(request));

    verify(traceStateService).markTtlExpired(currentState);
  }

  @Test
  void ingest_differentNameAfterDeadline_isAcceptedAndFlowContinues() {
    EventRequest request = newRequest("evt-7", "trace-1", "shipment.created", "delivered", 3600, Boolean.FALSE);
    when(eventRepository.existsByEventId("evt-7")).thenReturn(false);
    TraceState currentState = createTraceState(TraceStatus.WAITING_OTHER_EVENT, "payment.captured");
    when(traceStateService.findById("trace-1")).thenReturn(currentState);
    when(traceStateService.isTtlExpired(currentState)).thenReturn(true);
    TraceState newState = createTraceState(TraceStatus.WAITING_OTHER_EVENT, "delivered");
    when(traceStateService.save(any(), any())).thenReturn(newState);

    EventResponse response = eventService.ingest(request);

    assertThat(response.status()).isEqualTo(TraceStatus.WAITING_OTHER_EVENT);
    verify(eventRepository).save(any(Event.class));
    verify(traceStateService).save(eq(currentState), any(Event.class));
    verify(traceStateService, never()).markTtlExpired(any());
  }

  @Test
  void ingest_differentNameAfterDeadlineAndFinalEvent_isAcceptedAndStateCompleted() {
    EventRequest request = newRequest("evt-7", "trace-1", "shipment.created", "delivered", 3600, Boolean.TRUE);
    when(eventRepository.existsByEventId("evt-7")).thenReturn(false);
    TraceState currentState = createTraceState(TraceStatus.TTL_EXPIRED_FOR_EVENT, "payment.captured");
    when(traceStateService.findById("trace-1")).thenReturn(currentState);
    when(traceStateService.isTtlExpired(currentState)).thenReturn(true);
    TraceState newState = createTraceState(TraceStatus.COMPLETED, "delivered");
    when(traceStateService.save(any(), any())).thenReturn(newState);

    EventResponse response = eventService.ingest(request);

    assertThat(response.status()).isEqualTo(TraceStatus.COMPLETED);
    verify(eventRepository).save(any(Event.class));
    verify(traceStateService).save(eq(currentState), any(Event.class));
    verify(traceStateService, never()).markTtlExpired(any());
  }

  @Test
  void ingest_validEvent_persistsEventBeforeUpdatingTraceState() {
    EventRequest request = newRequest("evt-7", "trace-1", "order.created", null, null, Boolean.FALSE);
    when(eventRepository.existsByEventId("evt-7")).thenReturn(false);
    when(traceStateService.findById("trace-1")).thenReturn(null);
    TraceState newState = createTraceState(TraceStatus.STARTED, null);
    when(traceStateService.save(any(), any())).thenReturn(newState);

    eventService.ingest(request);

    ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventCaptor.capture());
    Event savedEvent = eventCaptor.getValue();
    assertThat(savedEvent.getEventId()).isEqualTo("evt-7");
    assertThat(savedEvent.getTraceId()).isEqualTo("trace-1");
  }
}
