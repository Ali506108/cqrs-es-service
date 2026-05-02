package com.saga.movie_service.domain;

import java.time.Instant;
import java.util.UUID;

public record MovieCreatedEvent(
        UUID aggregateId,
        String title,
        String description,
        String movieUrl,
        String promoUrl,
        UUID directorId,
        Instant release,
        Instant occurredAt,
        int version
) implements MovieEvent {

    @Override
    public int getVersion() {
        return version;
    }
}
