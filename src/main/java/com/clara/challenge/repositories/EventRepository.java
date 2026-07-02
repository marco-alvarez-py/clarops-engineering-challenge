package com.clara.challenge.repositories;

import com.clara.challenge.entities.Event;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, UUID> {

  boolean existsByEventId(String eventId);
}
