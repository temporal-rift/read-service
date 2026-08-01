package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.UUID;

record PlayerAbandonedPayload(UUID gameId, UUID playerId) {}
