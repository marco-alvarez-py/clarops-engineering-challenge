package com.clara.challenge.exceptions;

public class TraceAlreadyCompletedException extends RuntimeException {

  public TraceAlreadyCompletedException(String traceId) {
    super("Trace '%s' is already completed and does not accept new events".formatted(traceId));
  }
}
