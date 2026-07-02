package com.clara.challenge.repositories;

import com.clara.challenge.entities.Health;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthRepository extends JpaRepository<Health, Long> {}
