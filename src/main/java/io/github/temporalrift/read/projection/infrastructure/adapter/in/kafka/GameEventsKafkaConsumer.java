package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import io.github.temporalrift.read.projection.domain.port.out.ProcessedEventPort;

/** Consumes {@code game.events} (session/action/scoring facts from game-service). */
@Component
class GameEventsKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(GameEventsKafkaConsumer.class);
    private static final String CONSUMER = "projection.game-events";

    private final ProcessedEventPort processedEvents;

    GameEventsKafkaConsumer(ProcessedEventPort processedEvents) {
        this.processedEvents = processedEvents;
    }

    @KafkaListener(topics = "game.events", groupId = "read-service." + CONSUMER)
    void handle(Message<Object> message) {
        InboundEventClaim.accept(message, CONSUMER, processedEvents)
                .ifPresent(eventId -> log.info("Consumed game.events event {}", eventId));
    }
}
