package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.List;
import java.util.UUID;

record FactionRevealedPayload(UUID gameId, List<Reveal> reveals) {

    record Reveal(UUID playerId, String faction) {}
}
