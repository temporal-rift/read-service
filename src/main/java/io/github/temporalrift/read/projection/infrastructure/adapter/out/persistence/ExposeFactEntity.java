package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import io.github.temporalrift.read.projection.domain.model.ExposeFact;

@Entity
@Table(
        name = "game_expose_fact",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_game_expose_fact_identity",
                        columnNames = {"game_id", "era_number", "activist_player_id", "target_player_id", "round_number"
                        }))
class ExposeFactEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "era_number", nullable = false)
    private int eraNumber;

    @Column(name = "activist_player_id", nullable = false)
    private UUID activistPlayerId;

    @Column(name = "target_player_id", nullable = false)
    private UUID targetPlayerId;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "signature_type")
    private String signatureType;

    @Column(name = "signature_target_event_id")
    private UUID signatureTargetEventId;

    @Column(name = "signature_source_outcome_id")
    private UUID signatureSourceOutcomeId;

    @Column(name = "signature_target_outcome_id")
    private UUID signatureTargetOutcomeId;

    @Column(name = "behavior_changed", nullable = false)
    private boolean behaviorChanged;

    protected ExposeFactEntity() {}

    private ExposeFactEntity(UUID id, ExposeFact fact) {
        this.id = id;
        this.gameId = fact.gameId();
        this.eraNumber = fact.eraNumber();
        this.activistPlayerId = fact.activistPlayerId();
        this.targetPlayerId = fact.targetPlayerId();
        this.roundNumber = fact.roundNumber();
        updateFrom(fact);
    }

    static ExposeFactEntity fromDomain(UUID id, ExposeFact fact) {
        return new ExposeFactEntity(id, fact);
    }

    ExposeFact toDomain() {
        return new ExposeFact(
                gameId,
                eraNumber,
                activistPlayerId,
                targetPlayerId,
                roundNumber,
                signatureType,
                signatureTargetEventId,
                signatureSourceOutcomeId,
                signatureTargetOutcomeId,
                behaviorChanged);
    }

    void updateFrom(ExposeFact fact) {
        this.signatureType = fact.signatureType();
        this.signatureTargetEventId = fact.signatureTargetEventId();
        this.signatureSourceOutcomeId = fact.signatureSourceOutcomeId();
        this.signatureTargetOutcomeId = fact.signatureTargetOutcomeId();
        this.behaviorChanged = fact.behaviorChanged();
    }
}
