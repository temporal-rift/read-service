package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "game_band_correction",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_game_band_correction_identity",
                        columnNames = {"game_id", "era_number"}))
class BandCorrectionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "era_number", nullable = false)
    private int eraNumber;

    protected BandCorrectionEntity() {}

    BandCorrectionEntity(UUID id, UUID gameId, int eraNumber) {
        this.id = id;
        this.gameId = gameId;
        this.eraNumber = eraNumber;
    }
}
