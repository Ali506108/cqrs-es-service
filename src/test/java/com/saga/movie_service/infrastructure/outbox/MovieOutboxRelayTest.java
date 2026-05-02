package com.saga.movie_service.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.movie_service.domain.MovieCreatedEvent;
import com.saga.movie_service.domain.MovieEvent;
import com.saga.movie_service.domain.repo.EventStoreJpaRepository;
import com.saga.movie_service.domain.store.EventStoreEntity;
import com.saga.movie_service.infrastructure.kafka.producer.MovieEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieOutboxRelayTest {

    @Mock
    private EventStoreJpaRepository repository;

    @Mock
    private MovieEventPublisher publisher;

    @Test
    void relayShouldPublishPendingEventAndMarkItAsPublished() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        MovieOutboxRelay relay = new MovieOutboxRelay(repository, publisher, objectMapper);

        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        MovieEvent event = new MovieCreatedEvent(
                aggregateId,
                "Dune",
                "Epic sci-fi",
                "movie://dune",
                "promo://dune",
                UUID.randomUUID(),
                Instant.parse("2021-10-22T00:00:00Z"),
                Instant.parse("2026-05-03T10:00:00Z"),
                0
        );

        EventStoreEntity row = EventStoreEntity.builder()
                .id(eventId)
                .aggregateId(aggregateId)
                .aggregateType("MovieAggregate")
                .eventType("MovieCreatedEvent")
                .eventVersion(1)
                .payload(writeJson(objectMapper, event))
                .occurredAt(Instant.parse("2026-05-03T10:00:00Z"))
                .publishedAt(null)
                .build();

        when(repository.findTop50ByPublishedAtIsNullOrderByOccurredAtAsc()).thenReturn(List.of(row));
        when(publisher.publish(any(MovieEvent.class))).thenReturn(CompletableFuture.completedFuture(null));
        when(repository.findById(eventId)).thenReturn(Optional.of(row));

        relay.relayPendingEvents();

        assertNotNull(row.getPublishedAt());
        verify(repository).save(row);
    }

    private String writeJson(ObjectMapper objectMapper, MovieEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

