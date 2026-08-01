package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import io.github.temporalrift.read.projection.domain.port.out.ProcessedEventPort;

/** Consumes {@code timeline.events} (resolution facts from timeline-service). */
@Component
class TimelineEventsKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(TimelineEventsKafkaConsumer.class);
    private static final String CONSUMER = "projection.timeline-events";

    private final ProcessedEventPort processedEvents;

    TimelineEventsKafkaConsumer(ProcessedEventPort processedEvents) {
        this.processedEvents = processedEvents;
    }

    @KafkaListener(topics = "timeline.events", groupId = "read-service." + CONSUMER)
    void handle(Message<Object> message) {
        InboundEventClaim.accept(message, CONSUMER, processedEvents)
                .ifPresent(eventId -> log.info("Consumed timeline.events event {}", eventId));
    }
}
