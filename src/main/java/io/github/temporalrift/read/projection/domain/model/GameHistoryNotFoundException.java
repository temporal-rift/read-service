package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/** Raised when no era history exists for an authenticated game-history request. */
public final class GameHistoryNotFoundException extends RuntimeException {

    public GameHistoryNotFoundException(UUID gameId) {
        super("Game history not found for game " + gameId);
    }
}
