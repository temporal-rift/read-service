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

import io.github.temporalrift.read.projection.domain.model.ForesightPreview;
import io.github.temporalrift.read.projection.domain.model.ForesightPreviewEvent;

@Entity
@Table(
        name = "foresight_preview",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_foresight_preview_identity",
                        columnNames = {"game_id", "player_id", "era_number"}))
class ForesightPreviewEntity extends PlayerScopedBaseEntity {

    @Column(name = "next_era_number", nullable = false)
    private int nextEraNumber;

    /** The previewed events as a JSONB array; array order is the deck order. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "revealed_events", nullable = false, columnDefinition = "jsonb")
    private String revealedEvents;

    @Column(name = "empty_reason")
    private String emptyReason;

    protected ForesightPreviewEntity() {}

    private ForesightPreviewEntity(UUID id, ForesightPreview preview, ObjectMapper objectMapper) {
        super(id, preview.gameId(), preview.playerId(), preview.eraNumber());
        updateFrom(preview, objectMapper);
    }

    static ForesightPreviewEntity fromDomain(UUID id, ForesightPreview preview, ObjectMapper objectMapper) {
        return new ForesightPreviewEntity(id, preview, objectMapper);
    }

    ForesightPreview toDomain(ObjectMapper objectMapper) {
        return new ForesightPreview(
                getGameId(),
                getPlayerId(),
                getEraNumber(),
                nextEraNumber,
                Arrays.asList(objectMapper.readValue(revealedEvents, ForesightPreviewEvent[].class)),
                emptyReason);
    }

    void updateFrom(ForesightPreview preview, ObjectMapper objectMapper) {
        nextEraNumber = preview.nextEraNumber();
        revealedEvents = objectMapper.writeValueAsString(preview.revealedEvents());
        emptyReason = preview.emptyReason();
    }
}
