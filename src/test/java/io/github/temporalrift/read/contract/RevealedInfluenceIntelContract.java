package io.github.temporalrift.read.contract;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.RevealedInfluenceIntel;
import io.github.temporalrift.read.projection.domain.port.out.RevealedInfluenceIntelRepository;

public abstract class RevealedInfluenceIntelContract extends RevealedIntelContract<RevealedInfluenceIntel> {

    @Override
    protected abstract RevealedInfluenceIntelRepository port();

    @Override
    protected RevealedInfluenceIntel intel(
            UUID gameId, UUID viewer, int eraNumber, UUID eventId, int observedInRound, int marker) {
        return new RevealedInfluenceIntel(
                gameId,
                viewer,
                eraNumber,
                eventId,
                observedInRound,
                List.of(new UUID(8L, marker)),
                List.of(new UUID(8L, marker)));
    }
}
