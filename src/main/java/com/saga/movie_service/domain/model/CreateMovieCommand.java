package com.saga.movie_service.domain.model;

import java.time.Instant;
import java.util.UUID;

public record CreateMovieCommand (
        String title,
        String description,
        String movieUrl,
        String promoUrl,
        UUID directorId,
        Instant release
){
    
}