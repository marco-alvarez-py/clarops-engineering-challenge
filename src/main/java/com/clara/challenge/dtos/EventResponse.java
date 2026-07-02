package com.clara.challenge.dtos;

import com.clara.challenge.enums.TraceStatus;

public record EventResponse(String eventId, String traceId, TraceStatus status) {}
