package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "game_terminal_winner",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_game_terminal_winner_identity",
                        columnNames = {"game_id", "player_id"}))
class TerminalWinnerEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "faction", nullable = false)
    private String faction;

    protected TerminalWinnerEntity() {}

    TerminalWinnerEntity(UUID id, UUID gameId, UUID playerId, String faction) {
        this.id = id;
        this.gameId = gameId;
        this.playerId = playerId;
        this.faction = faction;
    }

    UUID getGameId() {
        return gameId;
    }

    UUID getPlayerId() {
        return playerId;
    }

    String getFaction() {
        return faction;
    }

    void setFaction(String faction) {
        this.faction = faction;
    }
}
