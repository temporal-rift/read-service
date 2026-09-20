package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/** Shared game/viewer/era identity for the per-player projection tables. */
@MappedSuperclass
abstract class PlayerScopedBaseEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "era_number", nullable = false)
    private int eraNumber;

    protected PlayerScopedBaseEntity() {}

    protected PlayerScopedBaseEntity(UUID id, UUID gameId, UUID playerId, int eraNumber) {
        this.id = id;
        this.gameId = gameId;
        this.playerId = playerId;
        this.eraNumber = eraNumber;
    }

    UUID getGameId() {
        return gameId;
    }

    UUID getPlayerId() {
        return playerId;
    }

    int getEraNumber() {
        return eraNumber;
    }
}
