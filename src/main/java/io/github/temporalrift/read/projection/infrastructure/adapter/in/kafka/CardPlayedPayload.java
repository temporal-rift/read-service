package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.UUID;

record CardPlayedPayload(
        UUID gameId,
        int eraNumber,
        int roundNumber,
        UUID playerId,
        UUID cardInstanceId,
        String cardType,
        UUID targetEventId,
        UUID sourceOutcomeId,
        UUID targetOutcomeId) {}
