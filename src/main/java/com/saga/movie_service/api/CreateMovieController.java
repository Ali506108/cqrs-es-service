package com.saga.movie_service.api;

import com.saga.movie_service.application.movieservice.CreateMovieHandler;
import com.saga.movie_service.domain.command.CreateMovieCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class CreateMovieController {

    private final CreateMovieHandler movieHandler;

    @PostMapping
    public CompletableFuture<ResponseEntity<UUID>> createMovie(@RequestBody @Valid CreateMovieCommand handler){
        return CompletableFuture.supplyAsync(() -> {
            UUID id = movieHandler.handle(handler);
            URI location = URI.create("/movies/" + id);
            return ResponseEntity.created(location).body(id);
        });
    }

}
