package io.github.temporalrift.read.projection.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.GameProjection;

public interface GameProjectionRepository {

    Optional<GameProjection> findByGameId(UUID gameId);

    /**
     * Ensures the game-scoped anchor exists, then reads it with a lock held for the transaction. This also
     * serializes cross-topic writers that arrive before {@code GameStarted} creates the normal projection.
     */
    Optional<GameProjection> findByGameIdForUpdate(UUID gameId);

    void save(GameProjection gameProjection);
}
