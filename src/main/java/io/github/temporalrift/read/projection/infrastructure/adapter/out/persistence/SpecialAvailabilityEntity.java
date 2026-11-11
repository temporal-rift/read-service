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

import io.github.temporalrift.read.projection.domain.model.SpecialAvailability;
import io.github.temporalrift.read.projection.domain.model.SpecialAvailabilityReport;

@Entity
@Table(
        name = "special_action_availability",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_special_action_availability_identity",
                        columnNames = {"game_id", "player_id"}))
class SpecialAvailabilityEntity extends PlayerScopedBaseEntity {

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    /** The specials as a JSONB array; array order is the order of the consumed fact. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specials", nullable = false, columnDefinition = "jsonb")
    private String specials;

    protected SpecialAvailabilityEntity() {}

    private SpecialAvailabilityEntity(UUID id, SpecialAvailabilityReport report, ObjectMapper objectMapper) {
        super(id, report.gameId(), report.playerId(), report.eraNumber());
        updateFrom(report, objectMapper);
    }

    static SpecialAvailabilityEntity fromDomain(UUID id, SpecialAvailabilityReport report, ObjectMapper objectMapper) {
        return new SpecialAvailabilityEntity(id, report, objectMapper);
    }

    SpecialAvailabilityReport toDomain(ObjectMapper objectMapper) {
        return new SpecialAvailabilityReport(
                getGameId(),
                getPlayerId(),
                getEraNumber(),
                roundNumber,
                Arrays.asList(objectMapper.readValue(specials, SpecialAvailability[].class)));
    }

    void updateFrom(SpecialAvailabilityReport report, ObjectMapper objectMapper) {
        setEraNumber(report.eraNumber());
        roundNumber = report.roundNumber();
        specials = objectMapper.writeValueAsString(report.specials());
    }
}
