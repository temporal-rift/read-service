package io.github.temporalrift.read.projection.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.GameHistoryProjection;

/** Driven persistence contract for the bounded per-era game-history projection. */
public interface GameHistoryRepository {

    Optional<GameHistoryProjection> findByGameIdAndEraNumber(UUID gameId, int eraNumber);

    /** @return an immutable snapshot ordered by ascending era number */
    List<GameHistoryProjection> findByGameId(UUID gameId);

    /**
     * Persists one era snapshot.
     *
     * @implSpec Implementations must preserve optimistic conflicts so concurrent topic updates retry rather
     *     than silently overwrite committed facts.
     */
    void save(GameHistoryProjection projection);
}
