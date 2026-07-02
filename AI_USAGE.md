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

