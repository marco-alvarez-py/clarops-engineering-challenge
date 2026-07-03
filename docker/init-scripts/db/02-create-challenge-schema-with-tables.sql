-- -------------------------
-- Schema defined for the solution
-- -------------------------
CREATE
  SCHEMA IF NOT EXISTS distributed_event_watchdog;
SET
search_path TO distributed_event_watchdog;

-- -------------------------
-- events
-- Immutable append-only log of every received event.
-- Kept in full (not just the latest) to support eventId
-- de-duplication, eventsReceived counts, and audit history.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS events(
      id UUID PRIMARY KEY,
      event_id VARCHAR(255) NOT NULL,
      trace_id VARCHAR(255) NOT NULL,
      event_name VARCHAR(255) NOT NULL,
      RESULT VARCHAR(10) NOT NULL,
      occurred_at TIMESTAMPTZ NOT NULL,
      received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      next_expected_event VARCHAR(255),
      next_event_ttl_seconds INTEGER,
      final_event BOOLEAN NOT NULL DEFAULT FALSE,
      metadata JSONB,
      CONSTRAINT uq_events_event_id UNIQUE(event_id),
      CONSTRAINT ck_events_result CHECK(
        RESULT IN(
          'SUCCESS',
          'ERROR'
        )
      ),
      CONSTRAINT ck_events_ttl_positive CHECK(
        next_event_ttl_seconds IS NULL
        OR next_event_ttl_seconds > 0
      )
    );

-- -------------------------
-- trace_state
-- Materialized projection of the latest event per traceId.
-- Upserted in the same transaction as the events insert so
-- GET /traces/{traceId}/status is a single indexed lookup
-- instead of replaying the full event log on every read.
-- TTL_EXPIRED_FOR_EVENT is detected lazily by comparing
-- next_expected_before to now() when the status is read, then
-- written back here so later reads don't need to recompute it.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS trace_state(
      trace_id VARCHAR(255) PRIMARY KEY,
      status VARCHAR(30) NOT NULL,
      last_event_id UUID NOT NULL REFERENCES events(id),
      last_event_name VARCHAR(255) NOT NULL,
      last_event_result VARCHAR(10) NOT NULL,
      next_expected_event VARCHAR(255),
      next_expected_before TIMESTAMPTZ,
      events_received INTEGER NOT NULL DEFAULT 1,
      created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      CONSTRAINT ck_trace_state_status CHECK(
        status IN(
          'STARTED',
          'WAITING_OTHER_EVENT',
          'TTL_EXPIRED_FOR_EVENT',
          'COMPLETED'
        )
      ),
      CONSTRAINT ck_trace_state_last_event_result CHECK(
        last_event_result IN(
          'SUCCESS',
          'ERROR'
        )
      ),
      CONSTRAINT ck_trace_state_events_received_positive CHECK(
        events_received > 0
      )
    );
