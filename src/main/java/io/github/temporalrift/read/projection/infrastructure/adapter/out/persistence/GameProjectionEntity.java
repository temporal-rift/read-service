package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.github.temporalrift.read.projection.domain.model.GameProjection;
import io.github.temporalrift.read.projection.domain.model.Phase;

@Entity
@Table(name = "game_projection")
class GameProjectionEntity {

    @Id
    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "era_number", nullable = false)
    private int eraNumber;

    @Column(name = "phase", nullable = false)
    private String phase;

    protected GameProjectionEntity() {}

    GameProjectionEntity(UUID gameId, int eraNumber, Phase phase) {
        this.gameId = gameId;
        this.eraNumber = eraNumber;
        this.phase = phase.name();
    }

    static GameProjectionEntity fromDomain(GameProjection domain) {
        return new GameProjectionEntity(domain.gameId(), domain.eraNumber(), domain.phase());
    }

    GameProjection toDomain() {
        return new GameProjection(gameId, eraNumber, Phase.valueOf(phase));
    }

    UUID getGameId() {
        return gameId;
    }

    void setEraNumber(int eraNumber) {
        this.eraNumber = eraNumber;
    }

    void setPhase(Phase phase) {
        this.phase = phase.name();
    }
}
