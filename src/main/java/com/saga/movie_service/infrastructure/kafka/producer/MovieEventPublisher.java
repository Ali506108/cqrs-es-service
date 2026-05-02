package com.saga.movie_service.infrastructure.kafka.producer;

import com.saga.movie_service.domain.MovieEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class MovieEventPublisher {

    private static final String TOPIC = "movie.event.created";
    private final KafkaTemplate<String , MovieEvent> kafkaTemplate;

    public CompletableFuture<SendResult<String , MovieEvent>> publish(MovieEvent event) {
        return kafkaTemplate.send(
                TOPIC,
                event.aggregateId().toString(),
                event
        );
    }
}
