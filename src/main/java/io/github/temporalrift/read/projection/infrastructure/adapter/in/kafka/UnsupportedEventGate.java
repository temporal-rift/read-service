package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

import io.github.temporalrift.read.shared.infrastructure.adapter.in.kafka.MessageHeaders;

/**
 * Detects an event type or envelope version a consumer does not support, before that record's
 * processed-event identity is claimed — so it stays replayable once a compatible consumer version exists.
 * A message with no {@code eventType} header at all is never "unsupported"; there is nothing to validate.
 */
final class UnsupportedEventGate {

    /** Every event type in this workspace is still at envelope schema version 1. */
    static final int CURRENT_ENVELOPE_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(UnsupportedEventGate.class);
    private static final String EVENT_TYPE_HEADER = "eventType";
    private static final String VERSION_HEADER = "version";

    private UnsupportedEventGate() {}

    static boolean isUnsupported(
            Message<?> message,
            String consumer,
            Set<String> knownEventTypes,
            int supportedVersion,
            KafkaSkipMetrics skipMetrics) {
        var eventType = MessageHeaders.asString(message, EVENT_TYPE_HEADER);
        if (eventType == null) {
            return false;
        }
        if (!knownEventTypes.contains(eventType)) {
            log.warn("Unsupported event type {} for consumer {} — skipping without claiming", eventType, consumer);
            skipMetrics.recordUnknownType();
            return true;
        }
        return isUnsupportedVersion(message, consumer, supportedVersion, skipMetrics);
    }

    static boolean isUnsupportedVersion(
            Message<?> message, String consumer, int supportedVersion, KafkaSkipMetrics skipMetrics) {
        var version = MessageHeaders.asString(message, VERSION_HEADER);
        if (String.valueOf(supportedVersion).equals(version)) {
            return false;
        }
        log.warn("Unsupported envelope version {} for consumer {} — skipping without claiming", version, consumer);
        skipMetrics.recordUnsupportedVersion(consumer);
        return true;
    }
}
