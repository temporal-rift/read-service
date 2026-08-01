package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.List;
import java.util.UUID;

record GameStartedPayload(UUID gameId, UUID lobbyId, List<UUID> playerIds, int totalFactions, int deckSize) {}
