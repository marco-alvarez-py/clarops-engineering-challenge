# AI Usage

## Tools Used

- Claude Code

## Prompts 1 - Init Claude Code

> /init This project is a challenge for a Sr. Enginner position at Clara. I need you to read carefully the README.md file to get the context of the challenge so you can help me to build a solution based on those premises

## Prompts 2 - Setup a UserPromptSubmit hook

> Set up a UserPromptSubmit hook in .claude/settings.json that logs every prompt I submit to AI_USAGE.md. Follow the format '## Prompts X - Short title' and under that line include the prompt used. Make it async so it doesn't  block my workflow. If the settings file already exists, merge this hook into it without overwriting existing configuration. You can see the "AI_USAGE.md Requirement" in the README.md file to understand what I need

## Prompts 3 - Give me at least 2 options for

> Give me at least 2 options for the data modeling for the events and statuses that could meet the requirements for the problem

## Prompts 4 - Yes, I agree. Let's go for the

> Yes, I agree. Let's go for the option B and update the DDL file

## Prompts 5 - I made some changes. First, in the

> I made some changes. First, in the docker db scripts I created another script to set a new schema and there I put the new tables. So the I kept the original schema as it is. Another change was the packages over the application project. Now it is more clear about the layered architecture, it has a controller, services, repositories and entities folders and I move the original files for health to those specific folders. Now, I need you to create the Entity, Repository, Service and Controller for the events and trace_state tables that we defined previously. Take into account to specify the schema (distributed_event_watchdog) in the @Table annotation of the Entity, also for the 'metadata' column use just a String data type with @JdbcTypeCode(SqlTypes.JSON)

## Prompts 6 - Update the business logic to support the

> Update the business logic to support the persistance of the state TTL_EXPIRED_FOR_EVENT, so future query to the same event doesn't need to calculate again the expiration time. Also, Add a validation that the expected event name match with the one that arrives. If not match throw an exception.

## Prompts 7 - I already set the field eventsReceived =

> I already set the field eventsReceived = 1 in the TraceState entity, so no need to set it in the EventIngestionService.java. You can continue with the previous request

## Prompts 8 - In the Entities, replace the use of

> In the Entities, replace the use of OffsetDateTime with Instant. This class is better for UTC timestamp as we used here. Update all the necessary classes that made use of it

## Prompts 9 - There is an edge case scenario where

> There is an edge case scenario where an event could arrive with one of this fields nextExpectedEvent or nextEventTtlSeconds with null value. This could break the business logic of the GET method if it persiste in that way. Run some tests with this case to verify this scenario. If my assumption is correct, implements a validation when we receive the event that should have both values (in case of one of them has been sent)

## Prompts 10 - Implement unit tests for the EventService, TraceStateService

> Implement unit tests for the EventService, TraceStateService and TraceStateTransitionResolver.
> Take into account the following suggestions:
> - Use the Roy Osherove style for all implementations: methodName_stateUnderTest_expectedBehavior
> - Each test should validate one business rule only.
> - Use Mockito if necessary.
> - Focus on test every state transitions possible, TTL expiration, final event behavior, duplicate events, and late events.

## Prompts 11 - There is a wrong behaviour when the

> There is a wrong behaviour when the late event arrives, match the expected name but TTL already expired. When TTL expired it always have to return an error. Make the fixes to the business logic and also in the tests

## Prompts 12 - We need to make some big changes

> We need to make some big changes, the TTL expiration is NOT a final state (as it is right now, it will not accept any new event on that trace). So adjust the business logic to the following logic:
> - A new event that arrives with the expected name and before nextExpectedBefore expires, is accepted (already implemented)
> - A new event that arrives with the expected name but after nextExpectedBefore expires, is rejected and the trace must be updated if is not already in the expiration state.
> - A new event that arrives with a name different than expected but before nextExpectedBefore expires, is rejected.
> - A new event that arrives with a name different than expected but after nextExpectedBefore expires, is accepted and the flow continues.

## Prompts 13 - In the hurl folder, create a new

> In the hurl folder, create a new file for the hurl test called 'full-flow.hurl' and in it create a full flow for and trace-id='full-trace-id' that calls to the POST /events following by the GET /traces/trace-id/status. The idea here is to start with an event that state in STARTED, then another event to move to WAITING_OTHER_EVENT, then another event that match the expected name but not final, and last an event that move the state to a COMPLETED state.

## Prompts 14 - Create all the hurl test necessary to

> Create all the hurl test necessary to cover each exceptions that were created. Separate it into differents files in the folder hurl.

## Prompts 15 - Check if the spring-boot app finished starting

> Check if the spring-boot app finished starting (Monitor task btccud80x / background bawarv0jo), then run the curl verification for the 5 new hurl exception scenarios, clean up test DB rows, and stop the app.

## Prompts 16 - Now create the hurl tests for the

> Now create the hurl tests for the states STARTED, WAITING_OTHER_EVENT and COMPLETED in independent files

## Prompts 17 - In the README.md file, prepare the structure

> In the README.md file, prepare the structure (do not complete the questions) about the all the decisions/trade-offs that this challenge posed.

## Prompts 18 - In the docker/init-scripts we created for the

> In the docker/init-scripts we created for the solution, validate if the index idx_events_trace_id_received_at is still useful. And create an index for the field event_id since it is used in the existsByEventId method.

## Accepted Suggestions

- Data model **Option B** (separate `events` append-only log + `trace_state` materialized projection table) over Option A (single mutable table), so `GET /traces/{traceId}/status` stays a single indexed lookup instead of replaying the event log, while `events` keeps full history for `eventId` de-duplication and audit.
- Added the `ConsistentNextEventFields` class-level bean validation constraint to reject requests where only one of `nextExpectedEvent` / `nextEventTtlSeconds` is set, after confirming via a scratch test that a partial pair would silently corrupt `TraceState` on read.
- Introduced `TtlExpiredException` (409) so a late event matching the expected name after the TTL deadline is always rejected, instead of being silently accepted.
- Reworked the TTL rule so `TTL_EXPIRED_FOR_EVENT` is not terminal: implemented the 4-case matrix (expected/different name × before/after deadline), including `@Transactional(noRollbackFor = TtlExpiredException.class)` on `EventService.ingest` so the eager `markTtlExpired` write survives the thrown exception in the same transaction.
- Roy Osherove-style unit tests (`methodName_stateUnderTest_expectedBehavior`) for `EventService`, `TraceStateService`, and `TraceStateTransitionResolver`, using Mockito with `eq()`/`any()` matcher combinations and `ArgumentCaptor` for verifying persisted entity state.
- Full hurl E2E coverage split into one file per custom exception (`duplicate-event-exception.hurl`, `trace-already-completed-exception.hurl`, `unexpected-event-exception.hurl`, `ttl-expired-exception.hurl`, `trace-not-found-exception.hurl`) and one file per trace status (`started-flow.hurl`, `waiting-other-event-flow.hurl`, `completed-flow.hurl`), plus `full-flow.hurl` for the end-to-end STARTED → WAITING_OTHER_EVENT → COMPLETED path.

## Rejected Suggestions

- Data modeling Option A (single mutable `trace_state`-only table with no separate event log) — rejected in favor of Option B to preserve full event history and support `eventId` de-duplication and `eventsReceived` counts.
- Keeping a `status` field on `EventResponse` alongside the Event entity fields.
- Adding a new explicit `CREATE INDEX` on `events.event_id` — rejected as redundant once confirmed that `CONSTRAINT uq_events_event_id UNIQUE(event_id)` already creates a backing unique btree index that `existsByEventId` uses.

## Manual Decisions

- Kept `eventsReceived` initialization (`= 1`) as a field default on the `TraceState` entity rather than setting it explicitly in `EventIngestionService`/`EventService`.
- Redesigned `EventResponse` to mirror the `Event` entity (minus `id`/`metadata`)
- Migrated all timestamp fields (`Event`, `TraceState`, DTOs) from `OffsetDateTime` to `Instant`, since every timestamp in this system is a UTC instant with no need to preserve an offset.

