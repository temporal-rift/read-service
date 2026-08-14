package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

public record HandCard(UUID cardInstanceId, String cardType, String grade) {

    public HandCard(UUID cardInstanceId, String cardType) {
        this(cardInstanceId, cardType, "I");
    }
}
