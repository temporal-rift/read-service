package io.github.temporalrift.read.projection.domain.model;

/** Signals that a participant's projection is still waiting for the authoritative game-start event. */
public class GameProjectionNotReadyException extends RuntimeException {

    public GameProjectionNotReadyException() {
        super("Game state is not ready. Retry shortly.");
    }
}
