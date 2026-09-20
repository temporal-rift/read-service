package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/** One public Rally/Momentum declaration of record for a game era — already broadcast to all participants. */
public record PublicDeclaration(
        UUID gameId, int eraNumber, UUID playerId, String mode, UUID targetEventId, UUID targetOutcomeId) {}
