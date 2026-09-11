package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.RevealedInfluenceIntel;

@Entity
@Table(
        name = "revealed_influence_intel",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_revealed_influence_intel_identity",
                        columnNames = {"game_id", "player_id", "era_number", "event_id"}))
class RevealedInfluenceIntelEntity extends RevealedIntelBaseEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "influencer_player_ids", nullable = false, columnDefinition = "jsonb")
    private String influencerPlayerIds;

    protected RevealedInfluenceIntelEntity() {}

    private RevealedInfluenceIntelEntity(UUID id, RevealedInfluenceIntel intel, ObjectMapper objectMapper) {
        super(id, intel.gameId(), intel.playerId(), intel.eraNumber(), intel.eventId(), intel.observedInRound());
        updateFrom(intel, objectMapper);
    }

    static RevealedInfluenceIntelEntity fromDomain(UUID id, RevealedInfluenceIntel intel, ObjectMapper objectMapper) {
        return new RevealedInfluenceIntelEntity(id, intel, objectMapper);
    }

    RevealedInfluenceIntel toDomain(ObjectMapper objectMapper) {
        List<UUID> influencers = Arrays.asList(objectMapper.readValue(influencerPlayerIds, UUID[].class));
        return new RevealedInfluenceIntel(
                getGameId(), getPlayerId(), getEraNumber(), getEventId(), getObservedInRound(), influencers);
    }

    void updateFrom(RevealedInfluenceIntel intel, ObjectMapper objectMapper) {
        setObservedInRound(intel.observedInRound());
        influencerPlayerIds = objectMapper.writeValueAsString(intel.influencerPlayerIds());
    }
}
