package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.github.temporalrift.read.projection.domain.model.GamePlayer;

@Entity
@Table(name = "game_player")
class GamePlayerEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "is_connected", nullable = false)
    private boolean isConnected;

    @Column(name = "faction")
    private String faction;

    protected GamePlayerEntity() {}

    GamePlayerEntity(UUID id, UUID gameId, UUID playerId, int score, boolean isConnected, String faction) {
        this.id = id;
        this.gameId = gameId;
        this.playerId = playerId;
        this.score = score;
        this.isConnected = isConnected;
        this.faction = faction;
    }

    static GamePlayerEntity fromDomain(UUID id, UUID gameId, GamePlayer domain) {
        return new GamePlayerEntity(
                id, gameId, domain.playerId(), domain.score(), domain.isConnected(), domain.faction());
    }

    GamePlayer toDomain() {
        return new GamePlayer(playerId, score, isConnected, faction);
    }

    UUID getId() {
        return id;
    }

    UUID getGameId() {
        return gameId;
    }

    UUID getPlayerId() {
        return playerId;
    }

    void setScore(int score) {
        this.score = score;
    }

    void setConnected(boolean connected) {
        isConnected = connected;
    }

    void setFaction(String faction) {
        this.faction = faction;
    }
}
