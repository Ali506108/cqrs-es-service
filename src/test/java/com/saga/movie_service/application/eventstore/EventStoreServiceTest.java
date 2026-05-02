package com.saga.movie_service.application.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saga.movie_service.domain.MovieCreatedEvent;
import com.saga.movie_service.domain.MovieEvent;
import com.saga.movie_service.domain.repo.EventStoreJpaRepository;
import com.saga.movie_service.domain.store.EventStoreEntity;
import com.saga.movie_service.exception.OptimisticLockingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventStoreServiceTest {

    @Mock
    private EventStoreJpaRepository repository;

    @Test
    void appendShouldPersistUnpublishedRowsWithIncrementedEventVersion() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventStoreService service = new EventStoreService(repository, objectMapper);

        UUID aggregateId = UUID.randomUUID();
        MovieEvent event = new MovieCreatedEvent(
                aggregateId,
                "Inception",
                "Dream thieves",
                "movie://inception",
                "promo://inception",
                UUID.randomUUID(),
                Instant.parse("2010-07-16T00:00:00Z"),
                Instant.parse("2026-05-03T10:00:00Z"),
                0
        );

        when(repository.countByAggregateId(aggregateId)).thenReturn(0L);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.append(aggregateId, List.of(event), 0);

        ArgumentCaptor<List<EventStoreEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        EventStoreEntity row = captor.getValue().get(0);
        assertEquals(aggregateId, row.getAggregateId());
        assertEquals("MovieAggregate", row.getAggregateType());
        assertEquals("MovieCreatedEvent", row.getEventType());
        assertEquals(1, row.getEventVersion());
        assertNull(row.getPublishedAt());
        assertNotNull(row.getPayload());
    }

    @Test
    void appendShouldFailOnOptimisticLockMismatch() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventStoreService service = new EventStoreService(repository, objectMapper);

        UUID aggregateId = UUID.randomUUID();
        when(repository.countByAggregateId(aggregateId)).thenReturn(2L);

        assertThrows(OptimisticLockingException.class,
                () -> service.append(aggregateId, List.of(), 0));
    }
}

