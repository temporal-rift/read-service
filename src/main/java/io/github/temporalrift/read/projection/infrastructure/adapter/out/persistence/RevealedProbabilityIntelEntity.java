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

import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityOutcome;

@Entity
@Table(
        name = "revealed_probability_intel",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_revealed_probability_intel_identity",
                        columnNames = {"game_id", "player_id", "era_number", "event_id"}))
class RevealedProbabilityIntelEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "era_number", nullable = false)
    private int eraNumber;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "observed_in_round", nullable = false)
    private int observedInRound;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "outcomes", nullable = false, columnDefinition = "jsonb")
    private String outcomes;

    protected RevealedProbabilityIntelEntity() {}

    private RevealedProbabilityIntelEntity(UUID id, RevealedProbabilityIntel intel, ObjectMapper objectMapper) {
        this.id = id;
        this.gameId = intel.gameId();
        this.playerId = intel.playerId();
        this.eraNumber = intel.eraNumber();
        this.eventId = intel.eventId();
        updateFrom(intel, objectMapper);
    }

    static RevealedProbabilityIntelEntity fromDomain(
            UUID id, RevealedProbabilityIntel intel, ObjectMapper objectMapper) {
        return new RevealedProbabilityIntelEntity(id, intel, objectMapper);
    }

    RevealedProbabilityIntel toDomain(ObjectMapper objectMapper) {
        return new RevealedProbabilityIntel(
                gameId,
                playerId,
                eraNumber,
                eventId,
                observedInRound,
                Arrays.asList(objectMapper.readValue(outcomes, RevealedProbabilityOutcome[].class)));
    }

    void updateFrom(RevealedProbabilityIntel intel, ObjectMapper objectMapper) {
        observedInRound = intel.observedInRound();
        outcomes = objectMapper.writeValueAsString(intel.outcomes());
    }

    int getObservedInRound() {
        return observedInRound;
    }
}
