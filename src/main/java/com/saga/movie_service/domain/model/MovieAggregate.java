package com.saga.movie_service.domain.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MovieAggregate {

    private UUID movieId;
    private String title;
    private String description;
    private UUID directorId;
    private Set<UUID> actorsIds = new HashSet<>();
    private Set<Genre> geners = new HashSet<>();
    private boolean created;
}
