# STARTED: the first event for a trace, carrying neither nextExpectedEvent/nextEventTtlSeconds
# nor finalEvent, simply starts the trace.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "state-started-evt-1",
  "traceId": "state-started-trace-id",
  "eventName": "order.created",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:00:00Z",
  "finalEvent": false
}

HTTP 201
[Asserts]
jsonpath "$.eventId" == "state-started-evt-1"
jsonpath "$.traceId" == "state-started-trace-id"
jsonpath "$.nextExpectedEvent" == null
jsonpath "$.finalEvent" == false

GET http://localhost:8080/api/traces/state-started-trace-id/status

HTTP 200
[Asserts]
jsonpath "$.traceId" == "state-started-trace-id"
jsonpath "$.status" == "STARTED"
jsonpath "$.lastEventName" == "order.created"
jsonpath "$.lastEventResult" == "SUCCESS"
jsonpath "$.nextExpectedEvent" == null
jsonpath "$.nextExpectedBefore" == null
jsonpath "$.eventsReceived" == 1