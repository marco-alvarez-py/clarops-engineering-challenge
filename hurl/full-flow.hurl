# Full flow for a single trace: STARTED -> WAITING_OTHER_EVENT -> STARTED (name matched, not
# final) -> COMPLETED. Each POST /events is followed by a GET /traces/{traceId}/status to assert
# the trace state was updated as expected.

# 1. First event for the trace, no next-expected fields -> trace starts as STARTED.
POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "full-trace-evt-1",
  "traceId": "full-trace-id",
  "eventName": "order.created",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:00:00Z",
  "finalEvent": false
}

HTTP 201
[Asserts]
jsonpath "$.eventId" == "full-trace-evt-1"
jsonpath "$.traceId" == "full-trace-id"
jsonpath "$.eventName" == "order.created"
jsonpath "$.result" == "SUCCESS"
jsonpath "$.nextExpectedEvent" == null
jsonpath "$.nextEventTtlSeconds" == null
jsonpath "$.finalEvent" == false

GET http://localhost:8080/api/traces/full-trace-id/status

HTTP 200
[Asserts]
jsonpath "$.traceId" == "full-trace-id"
jsonpath "$.status" == "STARTED"
jsonpath "$.lastEventName" == "order.created"
jsonpath "$.nextExpectedEvent" == null
jsonpath "$.eventsReceived" == 1

# 2. Second event declares the next expected event and its TTL -> trace moves to
# WAITING_OTHER_EVENT.
POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "full-trace-evt-2",
  "traceId": "full-trace-id",
  "eventName": "payment.initiated",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:05:00Z",
  "nextExpectedEvent": "payment.captured",
  "nextEventTtlSeconds": 2000000000,
  "finalEvent": false
}

HTTP 201
[Asserts]
jsonpath "$.eventId" == "full-trace-evt-2"
jsonpath "$.traceId" == "full-trace-id"
jsonpath "$.eventName" == "payment.initiated"
jsonpath "$.nextExpectedEvent" == "payment.captured"
jsonpath "$.nextEventTtlSeconds" == 2000000000
jsonpath "$.finalEvent" == false

GET http://localhost:8080/api/traces/full-trace-id/status

HTTP 200
[Asserts]
jsonpath "$.status" == "WAITING_OTHER_EVENT"
jsonpath "$.lastEventName" == "payment.initiated"
jsonpath "$.nextExpectedEvent" == "payment.captured"
jsonpath "$.nextExpectedBefore" != null
jsonpath "$.eventsReceived" == 2

# 3. Third event matches the expected name, arrives before the deadline, and is not final -> the
# trace is accepted and, since this event carries no next-expected fields of its own, moves back
# to STARTED.
POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "full-trace-evt-3",
  "traceId": "full-trace-id",
  "eventName": "payment.captured",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:10:00Z",
  "finalEvent": false
}

HTTP 201
[Asserts]
jsonpath "$.eventId" == "full-trace-evt-3"
jsonpath "$.traceId" == "full-trace-id"
jsonpath "$.eventName" == "payment.captured"
jsonpath "$.nextExpectedEvent" == null
jsonpath "$.nextEventTtlSeconds" == null
jsonpath "$.finalEvent" == false

GET http://localhost:8080/api/traces/full-trace-id/status

HTTP 200
[Asserts]
jsonpath "$.status" == "STARTED"
jsonpath "$.lastEventName" == "payment.captured"
jsonpath "$.nextExpectedEvent" == null
jsonpath "$.eventsReceived" == 3

# 4. Fourth event is final -> the trace completes regardless of any prior expectation.
POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "full-trace-evt-4",
  "traceId": "full-trace-id",
  "eventName": "shipment.delivered",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:15:00Z",
  "finalEvent": true
}

HTTP 201
[Asserts]
jsonpath "$.eventId" == "full-trace-evt-4"
jsonpath "$.traceId" == "full-trace-id"
jsonpath "$.eventName" == "shipment.delivered"
jsonpath "$.nextExpectedEvent" == null
jsonpath "$.nextEventTtlSeconds" == null
jsonpath "$.finalEvent" == true

GET http://localhost:8080/api/traces/full-trace-id/status

HTTP 200
[Asserts]
jsonpath "$.status" == "COMPLETED"
jsonpath "$.lastEventName" == "shipment.delivered"
jsonpath "$.nextExpectedEvent" == null
jsonpath "$.eventsReceived" == 4