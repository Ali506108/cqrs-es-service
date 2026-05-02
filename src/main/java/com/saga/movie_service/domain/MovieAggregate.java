package com.saga.movie_service.domain;

import com.saga.movie_service.domain.command.CreateMovieCommand;
import lombok.Getter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MovieAggregate {

    private UUID id;
    private String title;
    private boolean created;
    private final List<MovieEvent> changes = new ArrayList<>();
    @Getter
    // Version = number of persisted events that built the current state.
    // Uncommitted changes stay in `changes` and do not advance this counter yet.
    private int version = 0;

    public static MovieAggregate create(CreateMovieCommand movieCommand ) {
        var aggregate = new MovieAggregate();

        if (movieCommand.title() == null || movieCommand.title().isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }

        // The aggregate id must be created once and then reused across replay,
        // event store persistence, and outbox publishing. Do not generate it twice.
        var movieId = UUID.randomUUID();

        var event = new MovieCreatedEvent(
                movieId,
                movieCommand.title(),
                movieCommand.description(),
                movieCommand.movieUrl(),
                movieCommand.promoUrl(),
                movieCommand.directorId(),
                movieCommand.release(),
                Instant.now(),
                0
        );

        aggregate.apply(event);
        aggregate.changes.add(event);

        return aggregate;

    }

    public void apply(MovieCreatedEvent event) {
        if(this.created) {
            throw new IllegalStateException("Cannot apply movie to duplicate movie");
        }

        // Applying an event changes the entity state, but the persisted version is
        // advanced by the event store once the transaction is committed.
        this.id = event.aggregateId();
        this.title = event.title();
        this.created = true;
        this.version = 0;
    }

    public List<MovieEvent> pullChanges() {
        var copy = List.copyOf(changes);
        changes.clear();
        return copy;
    }

    public static MovieAggregate loadFromHistory(List<MovieEvent> events) {
        MovieAggregate aggregate = new MovieAggregate();

        for (MovieEvent event : events) {
            if (event instanceof MovieCreatedEvent createdEvent) {
                aggregate.apply(createdEvent);
                continue;
            }

            throw new IllegalStateException("Unsupported movie event: " + event.getClass().getName());
        }

        // Replay version must reflect how many persisted events built the current state.
        aggregate.version = events.size();
        aggregate.changes.clear();

        return aggregate;
    }

    public UUID movieId(){
        return id;
    }

}
