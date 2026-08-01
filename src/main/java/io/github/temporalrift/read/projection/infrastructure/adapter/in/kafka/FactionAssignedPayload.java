package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.UUID;

record FactionAssignedPayload(UUID gameId, UUID playerId, String faction) {}
