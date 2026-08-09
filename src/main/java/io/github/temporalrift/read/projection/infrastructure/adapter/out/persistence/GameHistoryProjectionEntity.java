package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Arrays;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.CascadedEventReference;
import io.github.temporalrift.read.projection.domain.model.GameHistoryProjection;
import io.github.temporalrift.read.projection.domain.model.HistoryEventDefinition;
import io.github.temporalrift.read.projection.domain.model.ResolvedOutcomeReference;

@Entity
@Table(
        name = "game_history_projection",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_game_history_projection_game_id_era_number",
                        columnNames = {"game_id", "era_number"}))
class GameHistoryProjectionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "era_number", nullable = false)
    private int eraNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_definitions", nullable = false, columnDefinition = "jsonb")
    private String eventDefinitions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resolved_outcome_references", nullable = false, columnDefinition = "jsonb")
    private String resolvedOutcomeReferences;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cascaded_event_references", nullable = false, columnDefinition = "jsonb")
    private String cascadedEventReferences;

    @Column(name = "cascaded_paradox_count", nullable = false)
    private int cascadedParadoxCount;

    @Column(name = "closed", nullable = false)
    private boolean closed;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected GameHistoryProjectionEntity() {}

    private GameHistoryProjectionEntity(UUID id, GameHistoryProjection domain, ObjectMapper objectMapper) {
        this.id = id;
        this.gameId = domain.gameId();
        this.eraNumber = domain.eraNumber();
        this.eventDefinitions = objectMapper.writeValueAsString(domain.eventDefinitions());
        this.resolvedOutcomeReferences = objectMapper.writeValueAsString(domain.resolvedOutcomeReferences());
        this.cascadedEventReferences = objectMapper.writeValueAsString(domain.cascadedEventReferences());
        this.cascadedParadoxCount = domain.cascadedParadoxCount();
        this.closed = domain.closed();
    }

    static GameHistoryProjectionEntity fromDomain(UUID id, GameHistoryProjection domain, ObjectMapper objectMapper) {
        return new GameHistoryProjectionEntity(id, domain, objectMapper);
    }

    GameHistoryProjection toDomain(ObjectMapper objectMapper) {
        return new GameHistoryProjection(
                gameId,
                eraNumber,
                Arrays.asList(objectMapper.readValue(eventDefinitions, HistoryEventDefinition[].class)),
                Arrays.asList(objectMapper.readValue(resolvedOutcomeReferences, ResolvedOutcomeReference[].class)),
                Arrays.asList(objectMapper.readValue(cascadedEventReferences, CascadedEventReference[].class)),
                cascadedParadoxCount,
                closed);
    }

    void updateFrom(GameHistoryProjection domain, ObjectMapper objectMapper) {
        eventDefinitions = objectMapper.writeValueAsString(domain.eventDefinitions());
        resolvedOutcomeReferences = objectMapper.writeValueAsString(domain.resolvedOutcomeReferences());
        cascadedEventReferences = objectMapper.writeValueAsString(domain.cascadedEventReferences());
        cascadedParadoxCount = domain.cascadedParadoxCount();
        closed = domain.closed();
    }

    long getVersion() {
        return version;
    }
}
