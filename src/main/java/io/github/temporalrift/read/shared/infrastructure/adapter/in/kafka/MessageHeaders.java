package io.github.temporalrift.read.shared.infrastructure.adapter.in.kafka;

import java.nio.charset.StandardCharsets;

import org.springframework.messaging.Message;

/**
 * Reads a header value as a {@code String} regardless of whether Spring Kafka's header mapper delivered it
 * as an already-converted {@code String} or as the raw {@code byte[]} record value.
 */
public final class MessageHeaders {

    private MessageHeaders() {}

    public static String asString(Message<?> message, String name) {
        return switch (message.getHeaders().get(name)) {
            case String value -> value;
            case byte[] value -> new String(value, StandardCharsets.UTF_8);
            case null, default -> null;
        };
    }
}
