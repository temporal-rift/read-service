package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "player_name")
class PlayerNameEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "player_name", nullable = false)
    private String playerName;

    protected PlayerNameEntity() {}

    PlayerNameEntity(UUID id, UUID gameId, UUID playerId, String playerName) {
        this.id = id;
        this.gameId = gameId;
        this.playerId = playerId;
        this.playerName = playerName;
    }

    UUID playerId() {
        return playerId;
    }

    String playerName() {
        return playerName;
    }

    void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
