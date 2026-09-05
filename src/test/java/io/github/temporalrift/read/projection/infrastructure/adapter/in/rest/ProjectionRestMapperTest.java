package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.domain.model.HandCard;
import io.github.temporalrift.read.projection.domain.model.Phase;

class ProjectionRestMapperTest {

    private static final UUID GAME_ID = UUID.randomUUID();

    @ParameterizedTest
    @CsvSource({
        "TRACE, ACTION_ROUND_1, 1, false",
        "TRACE, ACTION_ROUND_1, 2, true",
        "JAM, ACTION_ROUND_3, 2, false",
        "SCAN, ACTION_ROUND_3, 2, false",
        "INTERCEPT, ACTION_ROUND_3, 2, false",
        "JAM, ACTION_ROUND_1, 2, true",
        "JAM, ACTION_ROUND_2, 2, true",
        "STABILIZE, ACTION_ROUND_1, 2, false",
        "STABILIZE, ACTION_ROUND_2, 2, false",
        "STABILIZE, ACTION_ROUND_3, 2, false",
        "DETONATE, ACTION_ROUND_1, 2, false",
        "PUSH, ACTION_ROUND_1, 1, true",
        "PUSH, ACTION_ROUND_3, 2, true",
        "PUSH, PARADOX_RESOLUTION, 2, true"
    })
    void toResponse_setsIsPlayableThisRoundPerGddTable(String cardType, Phase phase, int eraNumber, boolean expected) {
        var card = new HandCard(UUID.randomUUID(), cardType);
        var result = resultWithHand(phase, eraNumber, List.of(card));

        var response = ProjectionRestMapper.toResponse(result);

        assertThat(response.getMyHand())
                .singleElement()
                .satisfies(handCard ->
                        assertThat(handCard.getIsPlayableThisRound()).isEqualTo(expected));
    }

    @Test
    void toResponse_flagFlipsAsPhaseAdvancesWithoutANewHand() {
        var card = new HandCard(UUID.randomUUID(), "SCAN");
        var round2 = ProjectionRestMapper.toResponse(resultWithHand(Phase.ACTION_ROUND_2, 1, List.of(card)));
        var round3 = ProjectionRestMapper.toResponse(resultWithHand(Phase.ACTION_ROUND_3, 1, List.of(card)));

        assertThat(round2.getMyHand().getFirst().getIsPlayableThisRound()).isTrue();
        assertThat(round3.getMyHand().getFirst().getIsPlayableThisRound()).isFalse();
    }

    private static GetPlayerGameStateUseCase.Result resultWithHand(Phase phase, int eraNumber, List<HandCard> hand) {
        return new GetPlayerGameStateUseCase.Result(
                GAME_ID, eraNumber, phase, "ERASERS", hand, null, 0, List.of(), List.of());
    }
}
