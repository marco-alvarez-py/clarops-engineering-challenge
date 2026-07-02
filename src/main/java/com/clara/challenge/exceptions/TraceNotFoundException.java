package com.clara.challenge.exceptions;

public class TraceNotFoundException extends RuntimeException {

  public TraceNotFoundException(String traceId) {
    super("Trace '%s' was not found".formatted(traceId));
  }
}
