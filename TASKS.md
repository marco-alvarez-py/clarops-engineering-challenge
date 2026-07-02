# Tasks

## Task 1 — Understand the problem and define a data model and DDL

- Analyze each possible scenario that the event could have.
- Define the tables with constraints and indexes required to support the solution.
- Add SQL initialization script for a new schema with the defined tables under `docker/init-scripts/db/`.

## Task 2 — Implement event ingestion contract

- Create the corresponding layered architecture for the event ingestion.
- Implement the repository layer with its Entities and Repositories
- Implement the service layer with the business logic.
- Implement the controller layer to handler `POST /events`.

