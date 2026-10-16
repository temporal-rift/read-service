package io.github.temporalrift.read.contract;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityOutcome;
import io.github.temporalrift.read.projection.domain.port.out.RevealedProbabilityIntelRepository;

public abstract class RevealedProbabilityIntelContract extends RevealedIntelContract<RevealedProbabilityIntel> {

    @Override
    protected abstract RevealedProbabilityIntelRepository port();

    @Override
    protected RevealedProbabilityIntel intel(
            UUID gameId, UUID viewer, int eraNumber, UUID eventId, int observedInRound, int marker) {
        return new RevealedProbabilityIntel(
                gameId,
                viewer,
                eraNumber,
                eventId,
                observedInRound,
                List.of(new RevealedProbabilityOutcome(new UUID(7L, marker), marker * 10, false, marker % 2 == 0)));
    }
}
