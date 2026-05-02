package com.saga.movie_service.infrastructure.outbox;

import com.saga.movie_service.domain.MovieCreatedEvent;
import com.saga.movie_service.domain.MovieEvent;
import com.saga.movie_service.domain.repo.EventStoreJpaRepository;
import com.saga.movie_service.domain.store.EventStoreEntity;
import com.saga.movie_service.infrastructure.kafka.producer.MovieEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieOutboxRelay {

    private final EventStoreJpaRepository repository;
    private final MovieEventPublisher publisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${movie-service.outbox.poll-delay-ms:1000}")
    public void relayPendingEvents() {
        List<EventStoreEntity> pending = repository.findTop50ByPublishedAtIsNullOrderByOccurredAtAsc();

        for (EventStoreEntity row : pending) {
            MovieEvent event = deserialize(row);

            publisher.publish(event)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("Outbox publish failed for event {} ({})", row.getId(), row.getEventType(), error);
                            return;
                        }

                        // Mark the row as published only after Kafka confirms the send.
                        // This is the key invariant that keeps the relay idempotent and restart-safe.
                        markPublished(row.getId());
                    });
        }
    }

    private MovieEvent deserialize(EventStoreEntity row) {
        if (!"MovieCreatedEvent".equals(row.getEventType())) {
            throw new IllegalStateException("Unsupported outbox event type: " + row.getEventType());
        }

        try {
            // Deserialize the original event payload (it may contain a placeholder version)
            MovieCreatedEvent original = objectMapper.readValue(row.getPayload(), MovieCreatedEvent.class);

            // Reconstruct the event object with the persisted event version from the event_store row.
            // This keeps the published event metadata consistent with the persisted store.
            return new MovieCreatedEvent(
                    original.aggregateId(),
                    original.title(),
                    original.description(),
                    original.movieUrl(),
                    original.promoUrl(),
                    original.directorId(),
                    original.release(),
                    original.occurredAt(),
                    row.getEventVersion()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize outbox payload for event " + row.getId(), e);
        }
    }

    protected void markPublished(UUID eventId) {
        repository.findById(eventId).ifPresent(row -> {
            if (row.getPublishedAt() == null) {
                row.markPublished(Instant.now());
                repository.save(row);
            }
        });
    }
}

