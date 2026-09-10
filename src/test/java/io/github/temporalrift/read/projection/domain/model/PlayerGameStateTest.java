package io.github.temporalrift.read.projection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerGameStateTest {

    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();

    @Test
    void effectiveJammedUntilRound_notJammed_isNull() {
        var state = new PlayerGameState(gameId, playerId, "ERASERS", List.of());

        assertThat(state.effectiveJammedUntilRound(1, Phase.ACTION_ROUND_1)).isNull();
    }

    @Test
    void effectiveJammedUntilRound_withinTheJammedRound_returnsIt() {
        var state = new PlayerGameState(gameId, playerId, "ERASERS", List.of(), null, 1, 2);

        assertThat(state.effectiveJammedUntilRound(1, Phase.ACTION_ROUND_1)).isEqualTo(2);
        assertThat(state.effectiveJammedUntilRound(1, Phase.ACTION_ROUND_2)).isEqualTo(2);
    }

    @Test
    void effectiveJammedUntilRound_pastTheJammedRound_isNull() {
        var state = new PlayerGameState(gameId, playerId, "ERASERS", List.of(), null, 1, 2);

        assertThat(state.effectiveJammedUntilRound(1, Phase.ACTION_ROUND_3)).isNull();
    }

    @Test
    void effectiveJammedUntilRound_afterActionRoundsClose_isNull() {
        var state = new PlayerGameState(gameId, playerId, "ERASERS", List.of(), null, 1, 3);

        assertThat(state.effectiveJammedUntilRound(1, Phase.RESOLUTION)).isNull();
        assertThat(state.effectiveJammedUntilRound(1, Phase.PARADOX_RESOLUTION)).isNull();
        assertThat(state.effectiveJammedUntilRound(1, Phase.ERA_END)).isNull();
    }

    @Test
    void effectiveJammedUntilRound_inALaterEra_isNull() {
        var state = new PlayerGameState(gameId, playerId, "ERASERS", List.of(), null, 1, 3);

        assertThat(state.effectiveJammedUntilRound(2, Phase.ACTION_ROUND_1)).isNull();
    }
}
