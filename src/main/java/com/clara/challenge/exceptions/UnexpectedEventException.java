package com.clara.challenge.exceptions;

public class UnexpectedEventException extends RuntimeException {

  public UnexpectedEventException(String traceId, String expectedEvent, String actualEvent) {
    super(
        "Trace '%s' expected event '%s' but received '%s'"
            .formatted(traceId, expectedEvent, actualEvent));
  }
}
