package com.saga.movie_service.application.eventstore;

import com.saga.movie_service.domain.MovieEvent;
import com.saga.movie_service.domain.repo.EventStoreJpaRepository;
import com.saga.movie_service.domain.store.EventStoreEntity;
import com.saga.movie_service.exception.OptimisticLockingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventStoreService {

    private final EventStoreJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void append(UUID aggregateId , List<MovieEvent> events ,  int expectedVersion)  {
        int curVersion = (int) repository.countByAggregateId(aggregateId);

        if(curVersion != expectedVersion) {
            throw new OptimisticLockingException(
                    String.format("Optimistic lock failed for aggregate %s. Expected version %d, but found %d",
                            aggregateId , expectedVersion, curVersion)
            );
        }

        List<EventStoreEntity> rows = new ArrayList<>();

        for (MovieEvent event : events) {
            if (!aggregateId.equals(event.aggregateId())) {
                throw new IllegalArgumentException("Event aggregateId must match the command aggregateId");
            }

            // The store assigns the persisted version; the event payload remains immutable.
            curVersion++;

            rows.add(
                    EventStoreEntity.builder()
                            .id(UUID.randomUUID())
                            .aggregateId(event.aggregateId())
                            .aggregateType("MovieAggregate")
                            .eventType(event.getClass().getSimpleName())
                            .eventVersion(curVersion)
                            .payload(toJson(event))
                            .occurredAt(event.occurredAt())
                            .publishedAt(null)
                            .build()
            );

        }

        repository.saveAll(rows);

    }

    private String toJson(Object event) {
        try{
            return objectMapper.writeValueAsString(event);
        }catch (Exception e){
            throw new IllegalStateException( "Failed to serialize event "+ e);
        }
    }
}
