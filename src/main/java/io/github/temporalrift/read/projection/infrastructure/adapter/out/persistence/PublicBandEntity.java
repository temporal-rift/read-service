package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Arrays;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.PublicBand;

@Entity
@Table(
        name = "game_public_band",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_game_public_band_identity",
                        columnNames = {"game_id", "era_number", "event_id"}))
class PublicBandEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "era_number", nullable = false)
    private int eraNumber;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "observed_in_round", nullable = false)
    private int observedInRound;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "outcomes", nullable = false, columnDefinition = "jsonb")
    private String outcomes;

    protected PublicBandEntity() {}

    private PublicBandEntity(UUID id, PublicBand band, ObjectMapper objectMapper) {
        this.id = id;
        this.gameId = band.gameId();
        this.eraNumber = band.eraNumber();
        this.eventId = band.eventId();
        this.observedInRound = band.observedInRound();
        this.outcomes = objectMapper.writeValueAsString(band.outcomes());
    }

    static PublicBandEntity fromDomain(UUID id, PublicBand band, ObjectMapper objectMapper) {
        return new PublicBandEntity(id, band, objectMapper);
    }

    PublicBand toDomain(ObjectMapper objectMapper) {
        return new PublicBand(
                gameId,
                eraNumber,
                eventId,
                observedInRound,
                Arrays.asList(objectMapper.readValue(outcomes, PublicBand.OutcomeBand[].class)));
    }

    UUID getEventId() {
        return eventId;
    }

    void updateFrom(PublicBand band, ObjectMapper objectMapper) {
        setObservedInRound(band.observedInRound());
        outcomes = objectMapper.writeValueAsString(band.outcomes());
    }

    private void setObservedInRound(int observedInRound) {
        this.observedInRound = observedInRound;
    }
}
