package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
class KafkaSkipMetrics {

    static final String METRIC_NAME = "read.kafka.consumer.skips";
    private static final String UNKNOWN_TYPE = "unknown_type";
    private static final String UNSUPPORTED_VERSION = "unsupported_version";

    private final MeterRegistry meterRegistry;

    KafkaSkipMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    void recordUnknownType() {
        meterRegistry.counter(METRIC_NAME, "reason", UNKNOWN_TYPE).increment();
    }

    void recordUnsupportedVersion(String consumer) {
        meterRegistry
                .counter(METRIC_NAME, "reason", UNSUPPORTED_VERSION, "consumer", consumer)
                .increment();
    }
}
