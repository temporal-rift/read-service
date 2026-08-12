package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.List;
import java.util.UUID;

record HandDealtPayload(UUID gameId, int eraNumber, UUID playerId, List<DealtCard> cards) {

    record DealtCard(UUID cardInstanceId, String cardType, String grade) {}
}
