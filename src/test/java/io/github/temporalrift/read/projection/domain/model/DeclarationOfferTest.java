package io.github.temporalrift.read.projection.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DeclarationOfferTest {

    @Test
    void offerWithoutAnEligibleMode_isRejected() {
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        List<DeclarationOffer.Mode> noModes = List.of();

        assertThatThrownBy(() -> new DeclarationOffer(gameId, playerId, 2, noModes))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
