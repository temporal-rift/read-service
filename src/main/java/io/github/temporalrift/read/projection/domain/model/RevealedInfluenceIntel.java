package io.github.temporalrift.read.projection.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * A tracing viewer's point-in-time influencer list for one traced event in one era, naming which of those
 * influencers acted through a Revisionist Mimic.
 */
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
            throw new IllegalArgumentException("Every Mimic influencer must also be an influencer");
        }
    }
}
