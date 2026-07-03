# DuplicateEventException: re-ingesting the same eventId is rejected with 409, regardless of
# whether the payload matches the original event.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "dup-evt-1",
  "traceId": "dup-trace-id",
  "eventName": "order.created",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:00:00Z",
  "finalEvent": false
}

HTTP 201
[Asserts]
jsonpath "$.eventId" == "dup-evt-1"

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "dup-evt-1",
  "traceId": "dup-trace-id",
  "eventName": "order.created",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:00:00Z",
  "finalEvent": false
}

HTTP 409
[Asserts]
jsonpath "$.error" == "Event with eventId 'dup-evt-1' already exists"