# TtlExpiredException: an event matching the expected name is still rejected once the deadline
# has passed (TTL expiration is not terminal, but the late arrival of the expected event itself
# is). The trace is eagerly flipped to TTL_EXPIRED_FOR_EVENT as a side effect of the rejection.
# occurredAt is set far in the past so the deadline (occurredAt + nextEventTtlSeconds) has already
# elapsed by the time this test runs, without needing to wait in real time.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "ttl-evt-1",
  "traceId": "ttl-trace-id",
  "eventName": "payment.initiated",
  "result": "SUCCESS",
  "occurredAt": "2020-01-01T00:00:00Z",
  "nextExpectedEvent": "payment.captured",
  "nextEventTtlSeconds": 3600,
  "finalEvent": false
}

HTTP 201
[Asserts]
jsonpath "$.eventId" == "ttl-evt-1"
jsonpath "$.nextExpectedEvent" == "payment.captured"

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "ttl-evt-2",
  "traceId": "ttl-trace-id",
  "eventName": "payment.captured",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:05:00Z",
  "finalEvent": false
}

HTTP 409
[Asserts]
jsonpath "$.error" == "Trace 'ttl-trace-id' already expired waiting for the next event"

GET http://localhost:8080/api/traces/ttl-trace-id/status

HTTP 200
[Asserts]
jsonpath "$.status" == "TTL_EXPIRED_FOR_EVENT"