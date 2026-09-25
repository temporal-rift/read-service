package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/**
 * One participant's public state within a game. {@code faction} is null until {@code FactionRevealed};
 * {@code playerName} is the lobby name, attached at read time and null when no join was observed.
 */
public record GamePlayer(UUID playerId, int score, boolean isConnected, String faction, String playerName) {

    public GamePlayer(UUID playerId, int score, boolean isConnected, String faction) {
        this(playerId, score, isConnected, faction, null);
    }

    public GamePlayer withPlayerName(String name) {
        return new GamePlayer(playerId, score, isConnected, faction, name);
    }
}
