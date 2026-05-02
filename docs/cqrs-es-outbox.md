# CQRS + Event Sourcing + Outbox for `movie-service`

This note describes the correct write-side flow for the current project and explains the bugs that were fixed in the previous implementation.

## 1. What the write side must guarantee

The write side is responsible for:

1. validating the command,
2. creating the aggregate,
3. producing domain events,
4. persisting those events in the event store,
5. publishing the same events to Kafka **only after the database transaction is committed**.

The command handler must not directly talk to Kafka.

## 2. The correct semantic flow

### Command side

`CreateMovieHandler`
- receives `CreateMovieCommand`
- creates `MovieAggregate`
- collects uncommitted events via `pullChanges()`
- appends events to `event_store`
- finishes the transaction

### Outbox relay

`MovieOutboxRelay`
- reads unpublished rows from `event_store`
- deserializes the event payload
- publishes the event to Kafka
- marks the row as published only after Kafka confirms the send

### Read side

A Kafka consumer on the read side should:
- consume the event
- build/update a MongoDB projection
- be idempotent

## 3. Bugs that were fixed in the current code

### Bug 1: direct Kafka publish inside the handler

Bad:
- the handler saves to the database and immediately sends to Kafka
- if the transaction rolls back, Kafka can still receive the event

Fixed:
- the handler only persists to the event store
- publishing is handled by the outbox relay after commit

### Bug 2: wrong entity field names

Bad field names such as:
- `aggregate_id`
- `event_version`

Spring Data derived queries expect Java property names, not raw column names.

Fixed:
- Java fields now use camelCase
- `@Column(name = "...")` maps them to snake_case columns

### Bug 3: broken replay version

Bad:
- `loadFromHistory()` tried to read version from `event.aggregateId().version()`

Fixed:
- replay version is derived from the number of persisted events
- unknown event types now fail fast

### Bug 4: aggregate id generated inconsistently

Bad:
- creating the aggregate id twice or hiding it inside event construction

Fixed:
- the aggregate id is generated once and reused across the command flow

## 4. Current domain rules

### `MovieAggregate`

Rules:
- `create()` must validate mandatory fields
- `pullChanges()` returns the uncommitted events and clears the buffer
- `loadFromHistory()` rebuilds state from persisted events
- `version` represents replay/persisted state, not a Kafka offset

### `MovieEvent`

Rules:
- an event must carry:
  - `aggregateId`
  - `occurredAt`
  - `version`
- the event payload should stay immutable

## 5. Outbox rules

Do:
- store the event in the database first
- publish from a separate relay
- mark the outbox row as published only after Kafka ack
- keep the relay idempotent
- for a single-node учебный проект, a scheduled relay is acceptable
- for multi-node production, add row claiming/locking so the same row is not published twice

Do not:
- publish directly from the command handler
- assume Kafka send success before the DB commit
- mutate the domain event after creation

## 6. JUnit tests to keep in the project

Recommended unit tests:

### `MovieAggregateTest`
Checks:
- aggregate creation
- title validation
- replay from history
- clearing of uncommitted changes

### `EventStoreServiceTest`
Checks:
- payload serialization
- event version assignment
- optimistic locking
- unpublished outbox state

## 7. How to extend the model safely

When adding a new event type:

1. add a new event record that implements `MovieEvent`
2. update `MovieEvent permits ...`
3. update `MovieAggregate.apply(...)`
4. update the outbox relay deserializer
5. add unit tests for the new event

## 8. Practical checklist before merging

- [ ] handler does not publish to Kafka directly
- [ ] event store entity uses camelCase Java properties
- [ ] repository derived queries match entity fields
- [ ] replay version is stable and deterministic
- [ ] outbox relay marks rows as published after Kafka ack
- [ ] unit tests cover aggregate and event store behavior

## 9. Summary

For a clean CQRS + Event Sourcing design, the write path must be deterministic, replayable, and side-effect free except for persistence. Kafka is a delivery mechanism, not part of the business transaction. The outbox relay bridges the gap safely.

