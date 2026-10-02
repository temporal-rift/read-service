package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import io.github.temporalrift.read.projection.domain.model.DeclarationWindow;

@Entity
@Table(
        name = "declaration_window",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_declaration_window_identity",
                        columnNames = {"game_id", "era_number"}))
class DeclarationWindowEntity {

    @Id
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "era_number", nullable = false)
    private int eraNumber;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected DeclarationWindowEntity() {}

    private DeclarationWindowEntity(UUID id, DeclarationWindow window) {
        this.id = id;
        gameId = window.gameId();
        eraNumber = window.eraNumber();
        expiresAt = window.expiresAt();
    }

    static DeclarationWindowEntity fromDomain(UUID id, DeclarationWindow window) {
        return new DeclarationWindowEntity(id, window);
    }

    DeclarationWindow toDomain() {
        return new DeclarationWindow(gameId, eraNumber, expiresAt);
    }
}
