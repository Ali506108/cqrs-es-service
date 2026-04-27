package com.saga.movie_service.domain.model;

import java.util.UUID;

public record AddActorToMovieCommand(
        UUID movieID,
        UUID actorId
){}