package com.clara.challenge.services;

import com.clara.challenge.entities.Health;
import com.clara.challenge.repositories.HealthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthService {

  private final HealthRepository healthRepository;

  public String getMessage() {
    return healthRepository.findAll().stream()
        .findFirst()
        .map(Health::getMessage)
        .orElse("unavailable");
  }
}
