package com.saga.movie_service.domain;

import com.saga.movie_service.domain.command.CreateMovieCommand;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovieAggregateTest {

    @Test
    void createShouldProduceOneChangeAndAStableMovieId() {
        var aggregate = MovieAggregate.create(command());

        assertNotNull(aggregate.movieId());
        assertEquals(0, aggregate.getVersion(), "A fresh aggregate has no persisted events yet");
        assertEquals(1, aggregate.pullChanges().size());
        assertEquals(0, aggregate.pullChanges().size(), "pullChanges must clear the uncommitted event buffer");
    }

    @Test
    void loadFromHistoryShouldRebuildStateAndReplayVersion() {
        UUID movieId = UUID.randomUUID();
        MovieCreatedEvent event = new MovieCreatedEvent(
                movieId,
                "Interstellar",
                "Sci-fi",
                "movie://interstellar",
                "promo://interstellar",
                UUID.randomUUID(),
                Instant.parse("2014-11-07T00:00:00Z"),
                Instant.parse("2026-05-03T10:00:00Z"),
                0
        );

        MovieAggregate aggregate = MovieAggregate.loadFromHistory(List.of(event));

        assertEquals(movieId, aggregate.movieId());
        assertEquals(1, aggregate.getVersion(), "Replay version must match persisted event count");
        assertEquals(0, aggregate.pullChanges().size(), "Replay must not leave uncommitted changes behind");
    }

    @Test
    void createShouldRejectBlankTitle() {
        var invalid = new CreateMovieCommand(
                " ",
                "desc",
                "movie://url",
                "promo://url",
                UUID.randomUUID(),
                Instant.now()
        );

        assertThrows(IllegalArgumentException.class, () -> MovieAggregate.create(invalid));
    }

    private CreateMovieCommand command() {
        return new CreateMovieCommand(
                "The Matrix",
                "Sci-fi classic",
                "movie://matrix",
                "promo://matrix",
                UUID.randomUUID(),
                Instant.parse("1999-03-31T00:00:00Z")
        );
    }
}

