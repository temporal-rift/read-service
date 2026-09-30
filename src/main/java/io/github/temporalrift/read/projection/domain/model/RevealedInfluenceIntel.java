package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/** A tracing viewer's point-in-time influencer list for one traced event in one era. */
public record RevealedInfluenceIntel(
        UUID gameId,
        UUID playerId,
        int eraNumber,
        UUID eventId,
        int observedInRound,
        List<UUID> influencerPlayerIds,
        List<UUID> mimicInfluencerPlayerIds)
        implements RevealedIntelEntry {

    public RevealedInfluenceIntel {
        influencerPlayerIds = List.copyOf(influencerPlayerIds);
        mimicInfluencerPlayerIds = List.copyOf(mimicInfluencerPlayerIds);
        if (!influencerPlayerIds.containsAll(mimicInfluencerPlayerIds)) {
            throw new IllegalArgumentException("Mimic influencers must also be listed as influencers");
        }
    }

    public RevealedInfluenceIntel(
            UUID gameId,
            UUID playerId,
            int eraNumber,
            UUID eventId,
            int observedInRound,
            List<UUID> influencerPlayerIds) {
        this(gameId, playerId, eraNumber, eventId, observedInRound, influencerPlayerIds, List.of());
    }
}
