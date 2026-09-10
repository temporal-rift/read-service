package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.domain.model.HandCard;
import io.github.temporalrift.read.projection.domain.model.LastRoundSummary;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityOutcome;
import io.github.temporalrift.read.projection.domain.model.RoundActionSummary;

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

    @Test
    void toResponse_alwaysMapsProbabilityIntelAsAList() {
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        var intel = new RevealedProbabilityIntel(
                GAME_ID,
                UUID.randomUUID(),
                2,
                eventId,
                2,
                List.of(new RevealedProbabilityOutcome(outcomeId, 60, false, true)));
        var result = new GetPlayerGameStateUseCase.Result(
                GAME_ID, 2, Phase.ACTION_ROUND_2, "ERASERS", List.of(), null, List.of(intel), 0, List.of(), List.of());

        var response = ProjectionRestMapper.toResponse(result);

        assertThat(response.getMyRevealedIntel()).singleElement().satisfies(revealed -> {
            assertThat(revealed.getKind().getValue()).isEqualTo("PROBABILITY");
            assertThat(revealed.getObservedInRound()).isEqualTo(2);
            assertThat(revealed.getEventId()).isEqualTo(eventId);
            assertThat(revealed.getOutcomes()).singleElement().satisfies(outcome -> {
                assertThat(outcome.getOutcomeId()).isEqualTo(outcomeId);
                assertThat(outcome.getProbability()).isEqualTo(60);
                assertThat(outcome.getIsAnnihilated()).isFalse();
                assertThat(outcome.getIsSealed()).isTrue();
            });
        });
    }

    @Test
    void toResponse_noIntelStillMapsAnEmptyList() {
        var response = ProjectionRestMapper.toResponse(resultWithHand(Phase.ACTION_ROUND_2, 2, List.of()));

        assertThat(response.getMyRevealedIntel()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "ERASERS, ANNIHILATE, CORRUPT, CASCADE",
        "PROPHETS, FORESIGHT, SEAL, FULFILLMENT",
        "REVISIONISTS, REWRITE, MIMIC, OBSCURE",
        "WEAVERS, THREAD, TAPESTRY, UNRAVEL",
        "ACTIVISTS, RALLY, EXPOSE, MOMENTUM"
    })
    void toResponse_setsMySpecialActionsPerFactionPerGddTable(
            String faction, String first, String second, String third) {
        var result = resultWithFaction(faction);

        var response = ProjectionRestMapper.toResponse(result);

        assertThat(response.getMySpecialActions()).containsExactly(first, second, third);
    }

    @Test
    void toResponse_nullFactionYieldsEmptyMySpecialActions() {
        var response = ProjectionRestMapper.toResponse(resultWithFaction(null));

        assertThat(response.getMySpecialActions()).isEmpty();
    }

    @Test
    void toResponse_mapsThePublicLastRoundSummary() {
        var playerId = UUID.randomUUID();
        var result = new GetPlayerGameStateUseCase.Result(
                GAME_ID,
                2,
                Phase.ACTION_ROUND_2,
                "ERASERS",
                List.of(),
                null,
                List.of(),
                0,
                List.of(),
                List.of(),
                new LastRoundSummary(2, 2, List.of(new RoundActionSummary(playerId, "INFORMATION", "CARD", false))),
                null);

        var response = ProjectionRestMapper.toResponse(result);

        assertThat(response.getLastRoundSummary()).satisfies(summary -> {
            assertThat(summary.getRoundNumber()).isEqualTo(2);
            assertThat(summary.getActionSummaries()).singleElement().satisfies(action -> {
                assertThat(action.getPlayerId()).isEqualTo(playerId);
                assertThat(action.getActionCategory()).isEqualTo("INFORMATION");
                assertThat(action.getActionFamily()).isEqualTo("CARD");
                assertThat(action.getSkipped()).isFalse();
            });
        });
    }

    @Test
    void toResponse_mapsMyJammedUntilRound() {
        var result = new GetPlayerGameStateUseCase.Result(
                GAME_ID,
                2,
                Phase.ACTION_ROUND_2,
                "ERASERS",
                List.of(),
                null,
                List.of(),
                0,
                List.of(),
                List.of(),
                null,
                3);

        var response = ProjectionRestMapper.toResponse(result);

        assertThat(response.getMyJammedUntilRound()).isEqualTo(3);
    }

    @Test
    void toResponse_nullJammedUntilRoundMapsToNull() {
        var response = ProjectionRestMapper.toResponse(resultWithHand(Phase.ACTION_ROUND_2, 2, List.of()));

        assertThat(response.getMyJammedUntilRound()).isNull();
    }

    private static GetPlayerGameStateUseCase.Result resultWithFaction(String faction) {
        return new GetPlayerGameStateUseCase.Result(
                GAME_ID, 2, Phase.ACTION_ROUND_1, faction, List.of(), null, List.of(), 0, List.of(), List.of());
    }

    private static GetPlayerGameStateUseCase.Result resultWithHand(Phase phase, int eraNumber, List<HandCard> hand) {
        return new GetPlayerGameStateUseCase.Result(
                GAME_ID, eraNumber, phase, "ERASERS", hand, null, List.of(), 0, List.of(), List.of());
    }
}
