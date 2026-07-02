package com.clara.challenge.exceptions;

public class TtlExpiredException extends RuntimeException {

  public TtlExpiredException(String traceId) {
    super("Trace '%s' already expired waiting for the next event".formatted(traceId));
  }
}
