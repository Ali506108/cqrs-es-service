package com.saga.movie_service.domain.model;

import java.util.UUID;

public record AddGenreToMovieCommand(
        UUID movieId,
        Genre genre
) {}

