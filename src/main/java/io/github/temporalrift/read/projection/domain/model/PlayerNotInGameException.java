package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/**
 * The requesting player never participated in the given game — surfaced as 404, not 403 (never discloses
 * existence).
 */
public class PlayerNotInGameException extends RuntimeException {

    public PlayerNotInGameException(UUID gameId, UUID playerId) {
        super("Player " + playerId + " is not a participant of game " + gameId);
    }
}
