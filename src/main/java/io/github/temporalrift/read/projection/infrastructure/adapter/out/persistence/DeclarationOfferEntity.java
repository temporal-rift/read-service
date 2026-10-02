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

import io.github.temporalrift.read.projection.domain.model.DeclarationOffer;

@Entity
@Table(
        name = "declaration_offer",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_declaration_offer_identity",
                        columnNames = {"game_id", "player_id", "era_number"}))
class DeclarationOfferEntity extends PlayerScopedBaseEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligible_modes", nullable = false, columnDefinition = "jsonb")
    private String eligibleModes;

    protected DeclarationOfferEntity() {}

    private DeclarationOfferEntity(UUID id, DeclarationOffer offer, ObjectMapper mapper) {
        super(id, offer.gameId(), offer.playerId(), offer.eraNumber());
        eligibleModes = mapper.writeValueAsString(offer.eligibleModes());
    }

    static DeclarationOfferEntity fromDomain(UUID id, DeclarationOffer offer, ObjectMapper mapper) {
        return new DeclarationOfferEntity(id, offer, mapper);
    }

    DeclarationOffer toDomain(ObjectMapper mapper) {
        return new DeclarationOffer(
                getGameId(),
                getPlayerId(),
                getEraNumber(),
                Arrays.asList(mapper.readValue(eligibleModes, DeclarationOffer.Mode[].class)));
    }
}
