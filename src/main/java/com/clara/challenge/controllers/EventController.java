package com.clara.challenge.controllers;

import com.clara.challenge.dtos.EventRequest;
import com.clara.challenge.dtos.EventResponse;
import com.clara.challenge.services.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

  private final EventService eventService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EventResponse ingest(@Valid @RequestBody EventRequest request) {
    return eventService.ingest(request);
  }
}
