# TraceAlreadyCompletedException: once a trace has a final event, it becomes COMPLETED and no
# longer accepts any further events, regardless of eventName.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "completed-evt-1",
  "traceId": "completed-trace-id",
  "eventName": "order.created",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:00:00Z",
  "finalEvent": true
}

HTTP 201
[Asserts]
jsonpath "$.eventId" == "completed-evt-1"
jsonpath "$.finalEvent" == true

GET http://localhost:8080/api/traces/completed-trace-id/status

HTTP 200
[Asserts]
jsonpath "$.status" == "COMPLETED"

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "completed-evt-2",
  "traceId": "completed-trace-id",
  "eventName": "refund.issued",
  "result": "SUCCESS",
  "occurredAt": "2026-07-02T10:05:00Z",
  "finalEvent": false
}

HTTP 409
[Asserts]
jsonpath "$.error" == "Trace 'completed-trace-id' is already completed and does not accept new events"