# WAITING_OTHER_EVENT: an event that declares both nextExpectedEvent and nextEventTtlSeconds
# moves the trace into a waiting state, with the deadline computed as occurredAt + ttlSeconds.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "state-waiting-evt-1",
  "traceId": "state-waiting-trace-id",
  "eventName": "payment.initiated",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:00:00Z",
  "nextExpectedEvent": "payment.captured",
  "nextEventTtlSeconds": 2000000000,
  "finalEvent": false
}

HTTP 201
[Asserts]
jsonpath "$.eventId" == "state-waiting-evt-1"
jsonpath "$.traceId" == "state-waiting-trace-id"
jsonpath "$.nextExpectedEvent" == "payment.captured"
jsonpath "$.nextEventTtlSeconds" == 2000000000
jsonpath "$.finalEvent" == false

GET http://localhost:8080/api/traces/state-waiting-trace-id/status

HTTP 200
[Asserts]
jsonpath "$.traceId" == "state-waiting-trace-id"
jsonpath "$.status" == "WAITING_OTHER_EVENT"
jsonpath "$.lastEventName" == "payment.initiated"
jsonpath "$.nextExpectedEvent" == "payment.captured"
jsonpath "$.nextExpectedBefore" != null
jsonpath "$.eventsReceived" == 1