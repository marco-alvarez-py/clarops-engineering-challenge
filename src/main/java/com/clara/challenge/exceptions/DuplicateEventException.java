package com.clara.challenge.exceptions;

public class DuplicateEventException extends RuntimeException {

  public DuplicateEventException(String eventId) {
    super("Event with eventId '%s' already exists".formatted(eventId));
  }
}
