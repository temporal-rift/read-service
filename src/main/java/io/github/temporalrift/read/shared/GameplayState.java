package io.github.temporalrift.read.shared;

/**
 * Gameplay state one module keeps for the games it handled: projections and idempotency claims. An isolated
 * simulation deployment owns all of it per execution context: it discards it when the context closes and opens a
 * context only over none.
 */
public interface GameplayState {

    /** Whether this module holds no state of any game. */
    boolean isEmpty();

    /** Discards the state of every game within the caller's transaction. */
    void discardAll();
}
