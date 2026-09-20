package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/**
 * One public Expose outcome for a game era. Round 2 carries the exposed Round-1 signature;
 * round 3 reports whether the target's behavior changed and never carries a signature.
 */
public record ExposeFact(
        UUID gameId,
        int eraNumber,
        UUID activistPlayerId,
        UUID targetPlayerId,
        int roundNumber,
        String signatureType,
        UUID signatureTargetEventId,
        UUID signatureSourceOutcomeId,
        UUID signatureTargetOutcomeId,
        boolean behaviorChanged) {}
