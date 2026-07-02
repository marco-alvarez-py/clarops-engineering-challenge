package com.clara.challenge.controllers;

import com.clara.challenge.services.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

  private final HealthService healthService;

  @GetMapping("/health")
  public String index() {
    return healthService.getMessage();
  }
}
