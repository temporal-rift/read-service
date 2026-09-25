package io.github.temporalrift.read.projection.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Game-scoped, shared-across-players state. {@code pendingParadoxIds} is the set of
 * paradoxes still open in the game's current {@code PARADOX_RESOLUTION} phase, if any — empty otherwise.
 *
 * <p>{@code revision} is a monotonic freshness marker for one game's projection only — it never promises
 * global or cross-topic event order. {@code currentRoundNumber} names the open action round, or the round
 * whose close opened paradox resolution; it is null outside playable phases.
 */
public record GameProjection(
        UUID gameId,
        int eraNumber,
        Phase phase,
        List<UUID> pendingParadoxIds,
        LastRoundSummary lastRoundSummary,
        Integer currentRoundNumber,
        Instant actionRoundExpiresAt,
        Instant paradoxResolutionExpiresAt,
        long revision,
        Instant lastUpdatedAt) {

    public GameProjection {
        pendingParadoxIds = List.copyOf(pendingParadoxIds);
    }

    public GameProjection(UUID gameId, int eraNumber, Phase phase) {
        this(gameId, eraNumber, phase, List.of(), null, null, null, null, 0, null);
    }

    public GameProjection(UUID gameId, int eraNumber, Phase phase, List<UUID> pendingParadoxIds) {
        this(gameId, eraNumber, phase, pendingParadoxIds, null, null, null, null, 0, null);
    }

    public GameProjection(
            UUID gameId, int eraNumber, Phase phase, List<UUID> pendingParadoxIds, LastRoundSummary lastRoundSummary) {
        this(gameId, eraNumber, phase, pendingParadoxIds, lastRoundSummary, null, null, null, 0, null);
    }
}
