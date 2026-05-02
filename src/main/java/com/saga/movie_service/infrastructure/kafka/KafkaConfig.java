package com.saga.movie_service.infrastructure.kafka;

import com.saga.movie_service.domain.MovieEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String , MovieEvent> kafkaProducerProperties() {
        Map<String , Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094");
        properties.put(ProducerConfig.ACKS_CONFIG , "all");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG , JsonSerializer.class);
        properties.put(ProducerConfig.RETRIES_CONFIG , Integer.MAX_VALUE);
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG , true);
        properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION , 5);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String , MovieEvent> kafkaTemplate(
            ProducerFactory<String , MovieEvent> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }
}
