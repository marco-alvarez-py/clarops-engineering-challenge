# UnexpectedEventException: while a trace is WAITING_OTHER_EVENT, an event whose name does not
# match nextExpectedEvent is rejected, as long as the deadline has not passed yet.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "unexpected-evt-1",
  "traceId": "unexpected-trace-id",
  "eventName": "payment.initiated",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:00:00Z",
  "nextExpectedEvent": "payment.captured",
  "nextEventTtlSeconds": 2000000000,
  "finalEvent": false
}

HTTP 201
[Asserts]
jsonpath "$.eventId" == "unexpected-evt-1"
jsonpath "$.nextExpectedEvent" == "payment.captured"

GET http://localhost:8080/api/traces/unexpected-trace-id/status

HTTP 200
[Asserts]
jsonpath "$.status" == "WAITING_OTHER_EVENT"
jsonpath "$.nextExpectedEvent" == "payment.captured"

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "unexpected-evt-2",
  "traceId": "unexpected-trace-id",
  "eventName": "shipment.created",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:05:00Z",
  "finalEvent": false
}

HTTP 409
[Asserts]
jsonpath "$.error" == "Trace 'unexpected-trace-id' expected event 'payment.captured' but received 'shipment.created'"