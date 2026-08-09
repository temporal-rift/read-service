package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Game-scoped, shared-across-players state (design.md Decision 1). {@code pendingParadoxIds} is the set of
 * paradoxes still open in the game's current {@code PARADOX_RESOLUTION} phase, if any — empty otherwise.
 */
public record GameProjection(UUID gameId, int eraNumber, Phase phase, List<UUID> pendingParadoxIds) {

    public GameProjection {
        pendingParadoxIds = List.copyOf(pendingParadoxIds);
    }

    public GameProjection(UUID gameId, int eraNumber, Phase phase) {
        this(gameId, eraNumber, phase, List.of());
    }
}
