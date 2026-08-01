package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.List;
import java.util.UUID;

record ScoresUpdatedPayload(UUID gameId, int eraNumber, List<ScoreUpdate> updates) {

    record ScoreUpdate(UUID playerId, String faction, int pointsDelta, String reason, int newTotal) {}
}
