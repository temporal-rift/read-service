package io.github.temporalrift.read.notification.domain.model;

import java.util.UUID;

public record NotificationRecipient(UUID gameId, UUID playerId) {}
