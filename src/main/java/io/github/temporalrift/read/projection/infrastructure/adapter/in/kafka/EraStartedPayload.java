package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.List;
import java.util.UUID;

record EraStartedPayload(UUID gameId, int eraNumber, List<UUID> cascadedEventIds, List<UUID> playerIds) {}
