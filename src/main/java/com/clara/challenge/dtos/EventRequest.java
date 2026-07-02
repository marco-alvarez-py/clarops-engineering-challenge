package com.clara.challenge.dtos;

import com.clara.challenge.enums.EventResult;
import com.clara.challenge.validation.ConsistentNextEventFields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

@ConsistentNextEventFields
public record EventRequest(
    @NotBlank String eventId,
    @NotBlank String traceId,
    @NotBlank String eventName,
    @NotNull EventResult result,
    @NotNull Instant occurredAt,
    String nextExpectedEvent,
    @Positive Integer nextEventTtlSeconds,
    Boolean finalEvent,
    JsonNode metadata) {}
