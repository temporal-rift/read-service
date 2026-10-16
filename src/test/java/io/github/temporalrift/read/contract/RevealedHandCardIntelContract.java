package io.github.temporalrift.read.contract;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.RevealedHandCard;
import io.github.temporalrift.read.projection.domain.model.RevealedHandCardIntel;
import io.github.temporalrift.read.projection.domain.port.out.RevealedHandCardIntelRepository;

public abstract class RevealedHandCardIntelContract extends RevealedIntelContract<RevealedHandCardIntel> {

    @Override
    protected abstract RevealedHandCardIntelRepository port();

    /** A hand intercept is identified by the player whose hand was observed, so {@code eventId} is that player. */
    @Override
    protected RevealedHandCardIntel intel(
            UUID gameId, UUID viewer, int eraNumber, UUID targetPlayerId, int observedInRound, int marker) {
        return new RevealedHandCardIntel(
                gameId,
                viewer,
                eraNumber,
                targetPlayerId,
                observedInRound,
                List.of(new RevealedHandCard(new UUID(6L, marker), "PUSH", "II")));
    }
}
