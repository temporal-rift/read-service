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

import io.github.temporalrift.read.projection.domain.model.ResolutionCardOffer;

@Entity
@Table(
        name = "resolution_card_offer",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_resolution_card_offer_identity",
                        columnNames = {"game_id", "player_id", "era_number"}))
class ResolutionCardOfferEntity extends PlayerScopedBaseEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cards", nullable = false, columnDefinition = "jsonb")
    private String cards;

    protected ResolutionCardOfferEntity() {}

    private ResolutionCardOfferEntity(UUID id, ResolutionCardOffer offer, ObjectMapper mapper) {
        super(id, offer.gameId(), offer.playerId(), offer.eraNumber());
        updateFrom(offer, mapper);
    }

    static ResolutionCardOfferEntity fromDomain(UUID id, ResolutionCardOffer offer, ObjectMapper mapper) {
        return new ResolutionCardOfferEntity(id, offer, mapper);
    }

    ResolutionCardOffer toDomain(ObjectMapper mapper) {
        return new ResolutionCardOffer(
                getGameId(),
                getPlayerId(),
                getEraNumber(),
                Arrays.asList(mapper.readValue(cards, ResolutionCardOffer.Card[].class)));
    }

    void updateFrom(ResolutionCardOffer offer, ObjectMapper mapper) {
        cards = mapper.writeValueAsString(offer.cards());
    }
}
