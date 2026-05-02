package com.saga.movie_service.application.movieservice;

import com.saga.movie_service.application.eventstore.EventStoreService;
import com.saga.movie_service.domain.MovieAggregate;
import com.saga.movie_service.domain.MovieEvent;
import com.saga.movie_service.domain.command.CreateMovieCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateMovieHandler {

    private final EventStoreService eventStore;

    @Transactional
    public UUID handle(CreateMovieCommand movieHandler){
        MovieAggregate aggregate = MovieAggregate.create(movieHandler);

        List<MovieEvent> events = aggregate.pullChanges();

        eventStore.append(aggregate.movieId() , events , aggregate.getVersion());

        return aggregate.movieId();

    }
}
