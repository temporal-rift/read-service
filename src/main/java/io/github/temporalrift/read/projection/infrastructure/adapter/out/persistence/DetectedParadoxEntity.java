package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Arrays;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.DetectedParadox;
import io.github.temporalrift.read.projection.domain.model.ParadoxType;

@Entity
@Table(name = "game_detected_paradox")
class DetectedParadoxEntity {

    @Id
    @Column(name = "paradox_id", nullable = false)
    private UUID paradoxId;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "era_number", nullable = false)
    private int eraNumber;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "affected_event_id", nullable = false)
    private UUID affectedEventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_outcome_ids", nullable = false, columnDefinition = "jsonb")
    private String affectedOutcomeIds;

    protected DetectedParadoxEntity() {}

    private DetectedParadoxEntity(DetectedParadox paradox, ObjectMapper objectMapper) {
        this.paradoxId = paradox.paradoxId();
        this.gameId = paradox.gameId();
        this.eraNumber = paradox.eraNumber();
        this.type = paradox.type().name();
        this.affectedEventId = paradox.affectedEventId();
        this.affectedOutcomeIds = objectMapper.writeValueAsString(paradox.affectedOutcomeIds());
    }

    static DetectedParadoxEntity fromDomain(DetectedParadox paradox, ObjectMapper objectMapper) {
        return new DetectedParadoxEntity(paradox, objectMapper);
    }

    DetectedParadox toDomain(ObjectMapper objectMapper) {
        return new DetectedParadox(
                gameId,
                eraNumber,
                paradoxId,
                ParadoxType.valueOf(type),
                affectedEventId,
                Arrays.asList(objectMapper.readValue(affectedOutcomeIds, UUID[].class)));
    }
}
