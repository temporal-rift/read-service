package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import io.github.temporalrift.read.projection.domain.model.PendingHandCard;

@Embeddable
record PlayerGameStatePendingHandCardValue(
        @Column(name = "card_instance_id", nullable = false) UUID cardInstanceId,
        @Column(name = "card_type", nullable = false) String cardType,
        @Column(name = "grade", nullable = false) String grade,
        @Column(name = "deal_slot", nullable = false) int dealSlot) {

    static PlayerGameStatePendingHandCardValue fromDomain(PendingHandCard card) {
        return new PlayerGameStatePendingHandCardValue(
                card.cardInstanceId(), card.cardType(), card.grade(), card.dealSlot());
    }

    PendingHandCard toDomain() {
        return new PendingHandCard(cardInstanceId, cardType, grade, dealSlot);
    }
}
