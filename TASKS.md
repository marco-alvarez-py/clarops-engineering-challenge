# Tasks

## Task 1 — Understand the problem and define a data model and DDL

- Analyze each possible scenario that the event could have.
- Define the tables with constraints and indexes required to support the solution.
- Add SQL initialization script for a new schema with the defined tables under `docker/init-scripts/db/`.

## Task 2 — Implement both endpoints

- Create the corresponding layered architecture for the proposed solution.
- Implement the repository layer with its Entities and Repositories
- Implement the service layer with the business logic.
- Implement the controllers to handler `POST /events` and `GET /traces/{traceId}/status`.
- Implement all the classes necessary to support the new endpoints.

## Task 3 — Implement the tests

- Implement unit test for the business logic.
- Implement hurl test for the endpoints.

## Task 4 — Documentations

- Finish all the documentation needed.

