package com.clara.challenge.services;

import com.clara.challenge.enums.TraceStatus;
import java.time.Instant;

public record TraceStateTransitionResult(
    TraceStatus status, String nextExpectedEvent, Instant nextExpectedBefore) {}
