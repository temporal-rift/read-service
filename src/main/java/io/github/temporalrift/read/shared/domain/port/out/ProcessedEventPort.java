package io.github.temporalrift.read.shared.domain.port.out;

import java.util.UUID;

/** Driven port backing idempotent claims for every logical Kafka consumer in read-service. */
public interface ProcessedEventPort {

    /** @return {@code true} only when this consumer newly claimed the event */
    boolean claim(UUID eventId, String consumer);
}
