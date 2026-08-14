package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import io.github.temporalrift.read.projection.domain.model.HandCard;

@Embeddable
record PlayerGameStateHandCardValue(
        @Column(name = "card_instance_id", nullable = false) UUID cardInstanceId,
        @Column(name = "card_type", nullable = false) String cardType,
        @Column(name = "grade", nullable = false) String grade) {

    static PlayerGameStateHandCardValue fromDomain(HandCard card) {
        return new PlayerGameStateHandCardValue(card.cardInstanceId(), card.cardType(), card.grade());
    }

    HandCard toDomain() {
        return new HandCard(cardInstanceId, cardType, grade);
    }
}
