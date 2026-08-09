package io.github.temporalrift.read.notification.infrastructure.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties("notification.websocket")
public record NotificationWebSocketProperties(
        List<String> allowedOrigins, Duration sendTimeLimit, DataSize sendBufferSize) {

    public NotificationWebSocketProperties {
        allowedOrigins = List.copyOf(allowedOrigins);
    }

    public int sendTimeLimitMillis() {
        return Math.toIntExact(sendTimeLimit.toMillis());
    }

    public int sendBufferSizeBytes() {
        return Math.toIntExact(sendBufferSize.toBytes());
    }
}
