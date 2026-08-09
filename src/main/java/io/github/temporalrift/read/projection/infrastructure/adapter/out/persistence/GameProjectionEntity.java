package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_projection_pending_paradox", joinColumns = @JoinColumn(name = "game_projection_id"))
    @Column(name = "paradox_id", nullable = false)
    private List<UUID> pendingParadoxIds;

    protected GameProjectionEntity() {}

    GameProjectionEntity(UUID gameId, int eraNumber, Phase phase, List<UUID> pendingParadoxIds) {
        this.gameId = gameId;
        this.eraNumber = eraNumber;
        this.phase = phase.name();
        this.pendingParadoxIds = pendingParadoxIds;
    }

    static GameProjectionEntity fromDomain(GameProjection domain) {
        return new GameProjectionEntity(
                domain.gameId(), domain.eraNumber(), domain.phase(), domain.pendingParadoxIds());
    }

    GameProjection toDomain() {
        return new GameProjection(gameId, eraNumber, Phase.valueOf(phase), pendingParadoxIds);
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

    void setPendingParadoxIds(List<UUID> pendingParadoxIds) {
        this.pendingParadoxIds = pendingParadoxIds;
    }
}
