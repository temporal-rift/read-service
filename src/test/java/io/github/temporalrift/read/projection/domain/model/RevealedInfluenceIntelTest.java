package io.github.temporalrift.read.projection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RevealedInfluenceIntelTest {

    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    @Test
    void mimicInfluencerListedAsInfluencer_isAccepted() {
        var cardInfluencer = UUID.randomUUID();
        var mimicInfluencer = UUID.randomUUID();

        var intel = new RevealedInfluenceIntel(
                gameId, playerId, 1, eventId, 2, List.of(cardInfluencer, mimicInfluencer), List.of(mimicInfluencer));

        assertThat(intel.influencerPlayerIds()).containsExactly(cardInfluencer, mimicInfluencer);
        assertThat(intel.mimicInfluencerPlayerIds()).containsExactly(mimicInfluencer);
    }

    @Test
    void emptyTrace_keepsBothListsEmpty() {
        var intel = new RevealedInfluenceIntel(gameId, playerId, 1, eventId, 2, List.of(), List.of());

        assertThat(intel.influencerPlayerIds()).isEmpty();
        assertThat(intel.mimicInfluencerPlayerIds()).isEmpty();
    }

    @Test
    void mimicInfluencerMissingFromInfluencers_isRejected() {
        var influencers = List.of(UUID.randomUUID());
        var mimicInfluencers = List.of(UUID.randomUUID());

        assertThatIllegalArgumentException()
                .isThrownBy(() ->
                        new RevealedInfluenceIntel(gameId, playerId, 1, eventId, 2, influencers, mimicInfluencers));
    }
}
