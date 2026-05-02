package com.saga.movie_service.domain.repo;

import com.saga.movie_service.domain.store.EventStoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventStoreJpaRepository extends JpaRepository<EventStoreEntity , UUID> {

    long countByAggregateId(UUID aggregateId);

    List<EventStoreEntity> findByAggregateIdOrderByEventVersionAsc(UUID aggregateId);

    List<EventStoreEntity> findTop50ByPublishedAtIsNullOrderByOccurredAtAsc();

}
