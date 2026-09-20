package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import io.github.temporalrift.read.projection.domain.model.PublicDeclaration;

@Entity
@Table(
        name = "game_declaration",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_game_declaration_identity",
                        columnNames = {"game_id", "era_number", "player_id"}))
class PublicDeclarationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "era_number", nullable = false)
    private int eraNumber;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "mode", nullable = false)
    private String mode;

    @Column(name = "target_event_id", nullable = false)
    private UUID targetEventId;

    @Column(name = "target_outcome_id", nullable = false)
    private UUID targetOutcomeId;

    protected PublicDeclarationEntity() {}

    private PublicDeclarationEntity(UUID id, PublicDeclaration declaration) {
        this.id = id;
        this.gameId = declaration.gameId();
        this.eraNumber = declaration.eraNumber();
        this.playerId = declaration.playerId();
        updateFrom(declaration);
    }

    static PublicDeclarationEntity fromDomain(UUID id, PublicDeclaration declaration) {
        return new PublicDeclarationEntity(id, declaration);
    }

    PublicDeclaration toDomain() {
        return new PublicDeclaration(gameId, eraNumber, playerId, mode, targetEventId, targetOutcomeId);
    }

    void updateFrom(PublicDeclaration declaration) {
        this.mode = declaration.mode();
        this.targetEventId = declaration.targetEventId();
        this.targetOutcomeId = declaration.targetOutcomeId();
    }
}
