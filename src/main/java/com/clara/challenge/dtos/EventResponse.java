package com.clara.challenge.dtos;

import com.clara.challenge.enums.EventResult;
import java.time.Instant;

public record EventResponse(
    String eventId,
    String traceId,
    String eventName,
    EventResult result,
    Instant occurredAt,
    Instant receivedAt,
    String nextExpectedEvent,
    Integer nextEventTtlSeconds,
    boolean finalEvent) {}
