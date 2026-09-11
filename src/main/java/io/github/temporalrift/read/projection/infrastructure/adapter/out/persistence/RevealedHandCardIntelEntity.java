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

import io.github.temporalrift.read.projection.domain.model.RevealedHandCard;
import io.github.temporalrift.read.projection.domain.model.RevealedHandCardIntel;

@Entity
@Table(
        name = "revealed_hand_card_intel",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_revealed_hand_card_intel_identity",
                        columnNames = {"game_id", "player_id", "era_number", "event_id"}))
class RevealedHandCardIntelEntity extends RevealedIntelBaseEntity {

    /**
     * The observed player's hand cards as JSONB. The identity {@code event_id} column
     * carries that observed player's ID: an intercept observes a hand, not a game event.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "revealed_cards", nullable = false, columnDefinition = "jsonb")
    private String revealedCards;

    protected RevealedHandCardIntelEntity() {}

    private RevealedHandCardIntelEntity(UUID id, RevealedHandCardIntel intel, ObjectMapper objectMapper) {
        super(id, intel.gameId(), intel.playerId(), intel.eraNumber(), intel.targetPlayerId(), intel.observedInRound());
        updateFrom(intel, objectMapper);
    }

    static RevealedHandCardIntelEntity fromDomain(UUID id, RevealedHandCardIntel intel, ObjectMapper objectMapper) {
        return new RevealedHandCardIntelEntity(id, intel, objectMapper);
    }

    RevealedHandCardIntel toDomain(ObjectMapper objectMapper) {
        List<RevealedHandCard> cards = Arrays.asList(objectMapper.readValue(revealedCards, RevealedHandCard[].class));
        return new RevealedHandCardIntel(
                getGameId(), getPlayerId(), getEraNumber(), getEventId(), getObservedInRound(), cards);
    }

    void updateFrom(RevealedHandCardIntel intel, ObjectMapper objectMapper) {
        setObservedInRound(intel.observedInRound());
        revealedCards = objectMapper.writeValueAsString(intel.revealedCards());
    }
}
