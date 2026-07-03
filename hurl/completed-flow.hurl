# COMPLETED: an event with finalEvent = true completes the trace, regardless of whether it also
# carries a nextExpectedEvent/nextEventTtlSeconds pair (final always wins).

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "state-completed-evt-1",
  "traceId": "state-completed-trace-id",
  "eventName": "shipment.delivered",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:00:00Z",
  "finalEvent": true
}

HTTP 201
[Asserts]
jsonpath "$.eventId" == "state-completed-evt-1"
jsonpath "$.traceId" == "state-completed-trace-id"
jsonpath "$.nextExpectedEvent" == null
jsonpath "$.finalEvent" == true

GET http://localhost:8080/api/traces/state-completed-trace-id/status

HTTP 200
[Asserts]
jsonpath "$.traceId" == "state-completed-trace-id"
jsonpath "$.status" == "COMPLETED"
jsonpath "$.lastEventName" == "shipment.delivered"
jsonpath "$.nextExpectedEvent" == null
jsonpath "$.nextExpectedBefore" == null
jsonpath "$.eventsReceived" == 1