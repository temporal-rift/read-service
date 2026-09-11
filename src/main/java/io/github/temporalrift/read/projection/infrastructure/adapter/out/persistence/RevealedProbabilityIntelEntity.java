package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Arrays;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
class RevealedProbabilityIntelEntity extends RevealedIntelBaseEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "outcomes", nullable = false, columnDefinition = "jsonb")
    private String outcomes;

    protected RevealedProbabilityIntelEntity() {}

    private RevealedProbabilityIntelEntity(UUID id, RevealedProbabilityIntel intel, ObjectMapper objectMapper) {
        super(id, intel.gameId(), intel.playerId(), intel.eraNumber(), intel.eventId(), intel.observedInRound());
        updateFrom(intel, objectMapper);
    }

    static RevealedProbabilityIntelEntity fromDomain(
            UUID id, RevealedProbabilityIntel intel, ObjectMapper objectMapper) {
        return new RevealedProbabilityIntelEntity(id, intel, objectMapper);
    }

    RevealedProbabilityIntel toDomain(ObjectMapper objectMapper) {
        return new RevealedProbabilityIntel(
                getGameId(),
                getPlayerId(),
                getEraNumber(),
                getEventId(),
                getObservedInRound(),
                Arrays.asList(objectMapper.readValue(outcomes, RevealedProbabilityOutcome[].class)));
    }

    void updateFrom(RevealedProbabilityIntel intel, ObjectMapper objectMapper) {
        setObservedInRound(intel.observedInRound());
        outcomes = objectMapper.writeValueAsString(intel.outcomes());
    }
}
