package com.clara.challenge.entities;

import com.clara.challenge.enums.EventResult;
import com.clara.challenge.enums.TraceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "distributed_event_watchdog", name = "trace_state")
public class TraceState {

  @Id
  @Column(name = "trace_id")
  private String traceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private TraceStatus status;

  @Column(name = "last_event_id", nullable = false)
  private UUID lastEventId;

  @Column(name = "last_event_name", nullable = false)
  private String lastEventName;

  @Enumerated(EnumType.STRING)
  @Column(name = "last_event_result", nullable = false, length = 10)
  private EventResult lastEventResult;

  @Column(name = "next_expected_event")
  private String nextExpectedEvent;

  @Column(name = "next_expected_before")
  private Instant nextExpectedBefore;

  @Column(name = "events_received", nullable = false)
  private int eventsReceived = 1;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
