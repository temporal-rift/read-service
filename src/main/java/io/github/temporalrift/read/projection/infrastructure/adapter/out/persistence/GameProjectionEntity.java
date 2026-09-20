package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import io.github.temporalrift.read.projection.domain.model.GameProjection;
import io.github.temporalrift.read.projection.domain.model.LastRoundSummary;
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

    @Column(name = "last_round_summary_round_number")
    private Integer lastRoundSummaryRoundNumber;

    @Column(name = "last_round_summary_era_number")
    private Integer lastRoundSummaryEraNumber;

    @Column(name = "current_round_number")
    private Integer currentRoundNumber;

    @Column(name = "action_round_expires_at")
    private Instant actionRoundExpiresAt;

    @Column(name = "paradox_resolution_expires_at")
    private Instant paradoxResolutionExpiresAt;

    @Column(name = "revision", nullable = false)
    private long revision;

    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_projection_last_round_summary_action", joinColumns = @JoinColumn(name = "game_id"))
    @OrderColumn(name = "action_position")
    private List<LastRoundSummaryActionValue> lastRoundSummaryActions;

    protected GameProjectionEntity() {}

    static GameProjectionEntity fromDomain(GameProjection domain) {
        var entity = new GameProjectionEntity();
        entity.setGameId(domain.gameId());
        entity.setEraNumber(domain.eraNumber());
        entity.setPhase(domain.phase());
        entity.setPendingParadoxIds(domain.pendingParadoxIds());
        entity.setLastRoundSummary(domain.lastRoundSummary());
        entity.setCurrentRoundNumber(domain.currentRoundNumber());
        entity.setActionRoundExpiresAt(domain.actionRoundExpiresAt());
        entity.setParadoxResolutionExpiresAt(domain.paradoxResolutionExpiresAt());
        entity.setRevision(domain.revision());
        entity.setLastUpdatedAt(domain.lastUpdatedAt());
        return entity;
    }

    GameProjection toDomain() {
        return new GameProjection(
                gameId,
                eraNumber,
                Phase.valueOf(phase),
                pendingParadoxIds,
                lastRoundSummaryRoundNumber == null
                        ? null
                        : new LastRoundSummary(
                                lastRoundSummaryEraNumber,
                                lastRoundSummaryRoundNumber,
                                lastRoundSummaryActions.stream()
                                        .map(LastRoundSummaryActionValue::toDomain)
                                        .toList()),
                currentRoundNumber,
                actionRoundExpiresAt,
                paradoxResolutionExpiresAt,
                revision,
                lastUpdatedAt);
    }

    UUID getGameId() {
        return gameId;
    }

    void setGameId(UUID gameId) {
        this.gameId = gameId;
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

    void setCurrentRoundNumber(Integer currentRoundNumber) {
        this.currentRoundNumber = currentRoundNumber;
    }

    void setActionRoundExpiresAt(Instant actionRoundExpiresAt) {
        this.actionRoundExpiresAt = actionRoundExpiresAt;
    }

    void setParadoxResolutionExpiresAt(Instant paradoxResolutionExpiresAt) {
        this.paradoxResolutionExpiresAt = paradoxResolutionExpiresAt;
    }

    long getRevision() {
        return revision;
    }

    void setRevision(long revision) {
        this.revision = revision;
    }

    void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    void setLastRoundSummary(LastRoundSummary lastRoundSummary) {
        lastRoundSummaryEraNumber = lastRoundSummary == null ? null : lastRoundSummary.eraNumber();
        lastRoundSummaryRoundNumber = lastRoundSummary == null ? null : lastRoundSummary.roundNumber();
        lastRoundSummaryActions = lastRoundSummary == null
                ? List.of()
                : lastRoundSummary.actionSummaries().stream()
                        .map(LastRoundSummaryActionValue::fromDomain)
                        .toList();
    }
}
