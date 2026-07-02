package com.clara.challenge.entities;

import com.clara.challenge.enums.EventResult;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "distributed_event_watchdog", name = "events")
public class Event {

  @Id private UUID id;

  @Column(name = "event_id", nullable = false)
  private String eventId;

  @Column(name = "trace_id", nullable = false)
  private String traceId;

  @Column(name = "event_name", nullable = false)
  private String eventName;

  @Enumerated(EnumType.STRING)
  @Column(name = "result", nullable = false, length = 10)
  private EventResult result;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(name = "next_expected_event")
  private String nextExpectedEvent;

  @Column(name = "next_event_ttl_seconds")
  private Integer nextEventTtlSeconds;

  @Column(name = "final_event", nullable = false)
  private boolean finalEvent;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private String metadata;
}
