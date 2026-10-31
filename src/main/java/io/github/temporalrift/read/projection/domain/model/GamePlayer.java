package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/**
 * One participant's public state within a game. {@code faction} is null until {@code FactionRevealed};
 * {@code playerName} comes from the {@code GameStarted} roster.
 *
 * @param seatIndex zero-based position in the {@code GameStarted} roster, which lists players in seat order; null until
 *     the roster is applied. A game's players are listed in this order.
 */
public record GamePlayer(
        UUID playerId, int score, boolean isConnected, String faction, String playerName, Integer seatIndex) {

    public GamePlayer {
        if (seatIndex != null && seatIndex < 0) {
            throw new IllegalArgumentException("seatIndex must not be negative");
        }
    }

    public GamePlayer(UUID playerId, int score, boolean isConnected, String faction, String playerName) {
        this(playerId, score, isConnected, faction, playerName, null);
    }

    public GamePlayer(UUID playerId, int score, boolean isConnected, String faction) {
        this(playerId, score, isConnected, faction, null);
    }

    public GamePlayer withScore(int newScore) {
        return new GamePlayer(playerId, newScore, isConnected, faction, playerName, seatIndex);
    }

    public GamePlayer withConnected(boolean connected) {
        return new GamePlayer(playerId, score, connected, faction, playerName, seatIndex);
    }

    public GamePlayer withFaction(String newFaction) {
        return new GamePlayer(playerId, score, isConnected, newFaction, playerName, seatIndex);
    }

    public GamePlayer withPlayerName(String name) {
        return new GamePlayer(playerId, score, isConnected, faction, name, seatIndex);
    }

    public GamePlayer withSeatIndex(int seat) {
        return new GamePlayer(playerId, score, isConnected, faction, playerName, seat);
    }
}
