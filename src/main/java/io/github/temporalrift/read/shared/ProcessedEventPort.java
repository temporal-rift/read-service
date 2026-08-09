package io.github.temporalrift.read.shared;

import java.util.UUID;

/** Public shared contract for idempotent read-service Kafka consumer claims. */
public interface ProcessedEventPort {

    boolean claim(UUID eventId, String consumer);
}
