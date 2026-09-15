package io.github.temporalrift.read.shared.infrastructure.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka consumer error handling for every {@code @KafkaListener} in this service. Without this, a poison
 * message (a payload that cannot be deserialized or mapped to its event type) would retry with the default
 * handler and then drop with only a log line once retries are exhausted. Instead we retry a few times for
 * transient faults and then park the record on the matching source topic's {@code .dlq} channel so it can be
 * investigated and replayed.
 */
@Configuration
class KafkaErrorHandlingConfig {

    private static final long RETRY_INTERVAL_MS = 1_000L;
    private static final long MAX_RETRIES = 2L;

    @Bean
    DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaOperations<Object, Object> kafkaOperations) {
        return new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (consumerRecord, exception) ->
                        new TopicPartition(consumerRecord.topic() + ".dlq", consumerRecord.partition()));
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        return new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES));
    }
}
