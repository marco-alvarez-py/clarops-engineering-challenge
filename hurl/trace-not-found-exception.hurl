# TraceNotFoundException: querying the status of a traceId that was never ingested returns 404.

GET http://localhost:8080/api/traces/does-not-exist-trace-id/status

HTTP 404
[Asserts]
jsonpath "$.error" == "Trace 'does-not-exist-trace-id' was not found"