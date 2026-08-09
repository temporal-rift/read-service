package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.UUID;

record ParadoxResolvedPayload(UUID gameId, int eraNumber, UUID paradoxId, UUID resolvedByPlayerId) {}
