package com.clara.challenge.dtos;

import com.clara.challenge.enums.EventResult;
import com.clara.challenge.enums.TraceStatus;
import java.time.Instant;

public record TraceStatusResponse(
    String traceId,
    TraceStatus status,
    String lastEventName,
    EventResult lastEventResult,
    String nextExpectedEvent,
    Instant nextExpectedBefore,
    int eventsReceived) {}
