package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/**
 * One participant's public state within a game. {@code faction} is null until {@code FactionRevealed};
 * {@code playerName} comes from the {@code GameStarted} roster.
 */
public record GamePlayer(UUID playerId, int score, boolean isConnected, String faction, String playerName) {

    public GamePlayer(UUID playerId, int score, boolean isConnected, String faction) {
        this(playerId, score, isConnected, faction, null);
    }

    public GamePlayer withScore(int newScore) {
        return new GamePlayer(playerId, newScore, isConnected, faction, playerName);
    }

    public GamePlayer withConnected(boolean connected) {
        return new GamePlayer(playerId, score, connected, faction, playerName);
    }

    public GamePlayer withFaction(String newFaction) {
        return new GamePlayer(playerId, score, isConnected, newFaction, playerName);
    }

    public GamePlayer withPlayerName(String name) {
        return new GamePlayer(playerId, score, isConnected, faction, name);
    }
}
