package com.saga.movie_service.domain;

import java.time.Instant;
import java.util.UUID;

public sealed interface MovieEvent permits MovieCreatedEvent {
    UUID aggregateId();
    Instant occurredAt();
    int getVersion();
}
