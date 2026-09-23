package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.domain.model.ChainStatus;
import io.github.temporalrift.read.projection.domain.model.ExposeFact;
import io.github.temporalrift.read.projection.domain.model.GameChain;
import io.github.temporalrift.read.projection.domain.model.HandCard;
import io.github.temporalrift.read.projection.domain.model.LastRoundSummary;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;
import io.github.temporalrift.read.projection.domain.model.PublicBand;
import io.github.temporalrift.read.projection.domain.model.PublicDeclaration;
import io.github.temporalrift.read.projection.domain.model.RevealedHandCard;
import io.github.temporalrift.read.projection.domain.model.RevealedHandCardIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedInfluenceIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityOutcome;
import io.github.temporalrift.read.projection.domain.model.RoundActionSummary;
import io.github.temporalrift.read.projection.domain.model.TerminalResult;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PlayerGameStateResponse;

class ProjectionRestMapperTest {

    private static final UUID GAME_ID = UUID.randomUUID();

    private static PlayerGameStateResponse toResponse(GetPlayerGameStateUseCase.Result result) {
        return ProjectionRestMapper.toResponse(result, 5);
    }

    private static PlayerGameStateResponse toResponse(GetPlayerGameStateUseCase.Result result, int maxEras) {
        return ProjectionRestMapper.toResponse(result, maxEras);
    }

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
        "STALL, ACTION_ROUND_1, 4, true",
        "STALL, ACTION_ROUND_1, 5, false",
        "STALL, ACTION_ROUND_3, 5, false",
        "PUSH, ACTION_ROUND_1, 1, true",
        "PUSH, ACTION_ROUND_3, 2, true",
        "PUSH, PARADOX_RESOLUTION, 2, true"
    })
    void toResponse_setsIsPlayableThisRoundPerGddTable(String cardType, Phase phase, int eraNumber, boolean expected) {
        var card = new HandCard(UUID.randomUUID(), cardType);
        var result = resultWithHand(phase, eraNumber, List.of(card));

        var response = toResponse(result);

        assertThat(response.getMyHand())
                .singleElement()
                .satisfies(handCard ->
                        assertThat(handCard.getIsPlayableThisRound()).isEqualTo(expected));
    }

    @Test
    void toResponse_flagFlipsAsPhaseAdvancesWithoutANewHand() {
        var card = new HandCard(UUID.randomUUID(), "SCAN");
        var round2 = toResponse(resultWithHand(Phase.ACTION_ROUND_2, 1, List.of(card)));
        var round3 = toResponse(resultWithHand(Phase.ACTION_ROUND_3, 1, List.of(card)));

        assertThat(round2.getMyHand().getFirst().getIsPlayableThisRound()).isTrue();
        assertThat(round3.getMyHand().getFirst().getIsPlayableThisRound()).isFalse();
    }

    @Test
    void toResponse_marksStallUnavailableAtConfiguredFinalEra() {
        var card = new HandCard(UUID.randomUUID(), "STALL");

        var beforeFinalEra = toResponse(resultWithHand(Phase.ACTION_ROUND_2, 1, List.of(card)), 2);
        var finalEra = toResponse(resultWithHand(Phase.ACTION_ROUND_2, 2, List.of(card)), 2);

        assertThat(beforeFinalEra.getMyHand().getFirst().getIsPlayableThisRound())
                .isTrue();
        assertThat(finalEra.getMyHand().getFirst().getIsPlayableThisRound()).isFalse();
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

        var response = toResponse(result);

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
        var response = toResponse(resultWithHand(Phase.ACTION_ROUND_2, 2, List.of()));

        assertThat(response.getMyRevealedIntel()).isEmpty();
    }

    @Test
    void toResponse_mapsInfluenceIntelWithItsInfluencers() {
        var eventId = UUID.randomUUID();
        var influencerOne = UUID.randomUUID();
        var influencerTwo = UUID.randomUUID();
        var intel = new RevealedInfluenceIntel(
                GAME_ID, UUID.randomUUID(), 2, eventId, 2, List.of(influencerOne, influencerTwo));
        var result = new GetPlayerGameStateUseCase.Result(
                GAME_ID, 2, Phase.ACTION_ROUND_2, "ERASERS", List.of(), null, List.of(intel), 0, List.of(), List.of());

        var response = toResponse(result);

        assertThat(response.getMyRevealedIntel()).singleElement().satisfies(revealed -> {
            assertThat(revealed.getKind().getValue()).isEqualTo("INFLUENCE");
            assertThat(revealed.getObservedInRound()).isEqualTo(2);
            assertThat(revealed.getEventId()).isEqualTo(eventId);
            assertThat(revealed.getInfluencerPlayerIds()).containsExactlyInAnyOrder(influencerOne, influencerTwo);
        });
    }

    @Test
    void toResponse_mapsHandCardIntelWithItsTargetAndCards() {
        var targetPlayerId = UUID.randomUUID();
        var cardInstanceId = UUID.randomUUID();
        var intel = new RevealedHandCardIntel(
                GAME_ID,
                UUID.randomUUID(),
                2,
                targetPlayerId,
                1,
                List.of(new RevealedHandCard(cardInstanceId, "SWING", "III")));
        var result = new GetPlayerGameStateUseCase.Result(
                GAME_ID, 2, Phase.ACTION_ROUND_2, "ERASERS", List.of(), null, List.of(intel), 0, List.of(), List.of());

        var response = toResponse(result);

        assertThat(response.getMyRevealedIntel()).singleElement().satisfies(revealed -> {
            assertThat(revealed.getKind().getValue()).isEqualTo("HAND_CARD");
            assertThat(revealed.getObservedInRound()).isEqualTo(1);
            assertThat(revealed.getEventId()).isEqualTo(targetPlayerId);
            assertThat(revealed.getTargetPlayerId()).isEqualTo(targetPlayerId);
            assertThat(revealed.getRevealedCards()).singleElement().satisfies(card -> {
                assertThat(card.getCardInstanceId()).isEqualTo(cardInstanceId);
                assertThat(card.getCardType()).isEqualTo("SWING");
                assertThat(card.getGrade().getValue()).isEqualTo("III");
            });
        });
    }

    @Test
    void toResponse_mapsEmptyHandCardIntelAsEmptyList() {
        var targetPlayerId = UUID.randomUUID();
        var intel = new RevealedHandCardIntel(GAME_ID, UUID.randomUUID(), 2, targetPlayerId, 1, List.of());
        var result = new GetPlayerGameStateUseCase.Result(
                GAME_ID, 2, Phase.ACTION_ROUND_2, "ERASERS", List.of(), null, List.of(intel), 0, List.of(), List.of());

        var response = toResponse(result);

        assertThat(response.getMyRevealedIntel()).singleElement().satisfies(revealed -> {
            assertThat(revealed.getKind().getValue()).isEqualTo("HAND_CARD");
            assertThat(revealed.getRevealedCards()).isEmpty();
        });
    }

    @Test
    void toResponse_mapsEmptyInfluenceIntelAsEmptyList() {
        var eventId = UUID.randomUUID();
        var intel = new RevealedInfluenceIntel(GAME_ID, UUID.randomUUID(), 2, eventId, 1, List.of());
        var result = new GetPlayerGameStateUseCase.Result(
                GAME_ID, 2, Phase.ACTION_ROUND_2, "ERASERS", List.of(), null, List.of(intel), 0, List.of(), List.of());

        var response = toResponse(result);

        assertThat(response.getMyRevealedIntel()).singleElement().satisfies(revealed -> {
            assertThat(revealed.getKind().getValue()).isEqualTo("INFLUENCE");
            assertThat(revealed.getInfluencerPlayerIds()).isEmpty();
        });
    }

    @ParameterizedTest
    @CsvSource({
        "ERASERS, ANNIHILATE, CORRUPT, CASCADE",
        "PROPHETS, FORESIGHT, SEAL, FULFILLMENT",
        "REVISIONISTS, REWRITE, MIMIC, OBSCURE",
        "WEAVERS, THREAD, TAPESTRY, REWEAVE",
        "ACTIVISTS, RALLY, EXPOSE, MOMENTUM"
    })
    void toResponse_setsMySpecialActionsPerFactionPerGddTable(
            String faction, String first, String second, String third) {
        var result = resultWithFaction(faction);

        var response = toResponse(result);

        assertThat(response.getMySpecialActions()).containsExactly(first, second, third);
    }

    @Test
    void toResponse_nullFactionYieldsEmptyMySpecialActions() {
        var response = toResponse(resultWithFaction(null));

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
                null,
                null);

        var response = toResponse(result);

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
                3,
                null);

        var response = toResponse(result);

        assertThat(response.getMyJammedUntilRound()).isEqualTo(3);
    }

    @Test
    void toResponse_nullJammedUntilRoundMapsToNull() {
        var response = toResponse(resultWithHand(Phase.ACTION_ROUND_2, 2, List.of()));

        assertThat(response.getMyJammedUntilRound()).isNull();
    }

    @Test
    void toResponse_mapsChainStatusAndLength() {
        var chainId = UUID.randomUUID();
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
                null,
                new GameChain(GAME_ID, chainId, ChainStatus.ACTIVE, 2));

        var response = toResponse(result);

        assertThat(response.getChain()).satisfies(chain -> {
            assertThat(chain.getStatus().getValue()).isEqualTo("ACTIVE");
            assertThat(chain.getLength()).isEqualTo(2);
        });
    }

    @Test
    void toResponse_noChainMapsToNull() {
        var response = toResponse(resultWithHand(Phase.ACTION_ROUND_2, 2, List.of()));

        assertThat(response.getChain()).isNull();
    }

    @Test
    void toResponse_mapsRecoverableCoordinatesDeadlinesAndContext() {
        var paradoxId = UUID.randomUUID();
        var result = new GetPlayerGameStateUseCase.Result(
                GAME_ID,
                2,
                Phase.PARADOX_RESOLUTION,
                "ERASERS",
                List.of(),
                null,
                List.of(),
                0,
                List.of(),
                List.of(),
                null,
                null,
                null,
                41,
                Instant.parse("2026-09-16T00:00:00Z"),
                3,
                null,
                null,
                Instant.parse("2026-09-16T00:05:00Z"),
                false,
                true,
                List.of(paradoxId),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null);

        var response = toResponse(result);

        assertThat(response.getRevision()).isEqualTo(41);
        assertThat(response.getLastUpdatedAt()).isEqualTo(OffsetDateTime.parse("2026-09-16T00:00:00Z"));
        assertThat(response.getRoundNumber()).isEqualTo(3);
        assertThat(response.getDeadlines().getHandSelectionExpiresAt()).isNull();
        assertThat(response.getDeadlines().getActionRoundExpiresAt()).isNull();
        assertThat(response.getDeadlines().getParadoxResolutionExpiresAt())
                .isEqualTo(OffsetDateTime.parse("2026-09-16T00:05:00Z"));
        assertThat(response.getPhaseContext().getDeclarationOpen()).isFalse();
        assertThat(response.getPhaseContext().getParadoxOpen()).isTrue();
        assertThat(response.getPhaseContext().getParadoxIds()).containsExactly(paradoxId);
    }

    @Test
    void toResponse_absentRecoverableListsMapToNull() {
        var response = toResponse(resultWithHand(Phase.ACTION_ROUND_2, 2, List.of()));

        assertThat(response.getPublicBands()).isNull();
        assertThat(response.getDeclarations()).isNull();
        assertThat(response.getExposeFacts()).isNull();
        assertThat(response.getMySubmissions()).isNull();
        assertThat(response.getResult()).isNull();
    }

    @Test
    void toResponse_mapsPublicBandsAndDeclarations() {
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        var declarer = UUID.randomUUID();
        var targetEventId = UUID.randomUUID();
        var targetOutcomeId = UUID.randomUUID();
        var result = resultWithRecoverable(
                List.of(new PublicBand(
                        GAME_ID, 2, eventId, 2, List.of(new PublicBand.OutcomeBand(outcomeId, "MEDIUM")))),
                List.of(new PublicDeclaration(GAME_ID, 2, declarer, "MOMENTUM", targetEventId, targetOutcomeId)),
                List.of(),
                List.of());

        var response = toResponse(result);

        assertThat(response.getPublicBands()).singleElement().satisfies(band -> {
            assertThat(band.getEventId()).isEqualTo(eventId);
            assertThat(band.getObservedInRound()).isEqualTo(2);
            assertThat(band.getOutcomes()).singleElement().satisfies(outcome -> {
                assertThat(outcome.getOutcomeId()).isEqualTo(outcomeId);
                assertThat(outcome.getBand().getValue()).isEqualTo("MEDIUM");
            });
        });
        assertThat(response.getDeclarations()).singleElement().satisfies(declaration -> {
            assertThat(declaration.getPlayerId()).isEqualTo(declarer);
            assertThat(declaration.getMode().getValue()).isEqualTo("MOMENTUM");
            assertThat(declaration.getTargetEventId()).isEqualTo(targetEventId);
            assertThat(declaration.getTargetOutcomeId()).isEqualTo(targetOutcomeId);
            assertThat(declaration.getEraNumber()).isEqualTo(2);
        });
    }

    @Test
    void toResponse_mapsExposeFactsAndOwnSubmissionsWithoutBudgetsOrProgress() {
        var activist = UUID.randomUUID();
        var target = UUID.randomUUID();
        var targetEventId = UUID.randomUUID();
        var targetOutcomeId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        var result = resultWithRecoverable(
                List.of(),
                List.of(),
                List.of(new ExposeFact(
                        GAME_ID, 2, activist, target, 2, "SWING", targetEventId, null, targetOutcomeId, false)),
                List.of(new PlayerSubmission(GAME_ID, playerId, 2, 1, PlayerSubmission.SubmissionKind.ACTION, "CARD")));

        var response = toResponse(result);

        assertThat(response.getExposeFacts()).singleElement().satisfies(fact -> {
            assertThat(fact.getActivistPlayerId()).isEqualTo(activist);
            assertThat(fact.getTargetPlayerId()).isEqualTo(target);
            assertThat(fact.getRoundNumber()).isEqualTo(2);
            assertThat(fact.getSignature().getType().getValue()).isEqualTo("SWING");
            assertThat(fact.getBehaviorChanged()).isFalse();
        });
        assertThat(response.getMySubmissions()).singleElement().satisfies(submission -> {
            assertThat(submission.getEraNumber()).isEqualTo(2);
            assertThat(submission.getRoundNumber()).isEqualTo(1);
            assertThat(submission.getKind().getValue()).isEqualTo("ACTION");
            assertThat(submission.getStatus().getValue()).isEqualTo("ACCEPTED");
            assertThat(submission.getActionType().getValue()).isEqualTo("CARD");
        });
        assertThat(response.getMySpecialBudgets()).isEmpty();
        assertThat(response.getMyObjectiveProgress()).isNull();
    }

    private static GetPlayerGameStateUseCase.Result resultWithRecoverable(
            List<PublicBand> bands,
            List<PublicDeclaration> declarations,
            List<ExposeFact> exposeFacts,
            List<PlayerSubmission> submissions) {
        return new GetPlayerGameStateUseCase.Result(
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
                null,
                null,
                7,
                null,
                2,
                null,
                null,
                null,
                false,
                false,
                List.of(),
                bands,
                declarations,
                exposeFacts,
                submissions,
                null);
    }

    @Test
    void toResponse_mapsAuthoritativeTerminalResult() {
        var winnerId = UUID.randomUUID();
        var result = new GetPlayerGameStateUseCase.Result(
                GAME_ID,
                2,
                Phase.GAME_ENDED,
                "ERASERS",
                List.of(),
                null,
                List.of(),
                20,
                List.of(),
                List.of(),
                null,
                null,
                null,
                99,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new TerminalResult(
                        GAME_ID,
                        "SCORE_THRESHOLD",
                        List.of(new TerminalResult.TerminalWinner(winnerId, "WEAVERS")),
                        List.of(new TerminalResult.TerminalScore(winnerId, "WEAVERS", 20))));

        var response = toResponse(result);

        assertThat(response.getResult()).satisfies(terminal -> {
            assertThat(terminal.getEndReason().getValue()).isEqualTo("SCORE_THRESHOLD");
            assertThat(terminal.getWinners()).singleElement().satisfies(winner -> {
                assertThat(winner.getPlayerId()).isEqualTo(winnerId);
                assertThat(winner.getFaction()).isEqualTo("WEAVERS");
            });
            assertThat(terminal.getFinalScores()).singleElement().satisfies(score -> {
                assertThat(score.getPlayerId()).isEqualTo(winnerId);
                assertThat(score.getScore()).isEqualTo(20);
            });
            assertThat(terminal.getRevealBoundary().getValue()).isEqualTo("FACTIONS_AND_SCORES_PUBLIC");
        });
    }

    @Test
    void toResponse_unmappableEndReasonOmitsResultRatherThanFabricatingOne() {
        var result = new GetPlayerGameStateUseCase.Result(
                GAME_ID,
                2,
                Phase.GAME_ENDED,
                "ERASERS",
                List.of(),
                null,
                List.of(),
                20,
                List.of(),
                List.of(),
                null,
                null,
                null,
                99,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new TerminalResult(GAME_ID, "FACTION_OBJECTIVE", List.of(), List.of()));

        var response = toResponse(result);

        assertThat(response.getResult()).isNull();
    }

    @Test
    void toResponse_winConditionMetWithUnanimousScoreThreshold_mapsScoreThreshold() {
        var winnerId = UUID.randomUUID();
        var response = toResponse(resultWithTerminal(new TerminalResult(
                GAME_ID,
                "WIN_CONDITION_MET",
                List.of(new TerminalResult.TerminalWinner(winnerId, "WEAVERS", "SCORE_THRESHOLD")),
                List.of(new TerminalResult.TerminalScore(winnerId, "WEAVERS", 20)))));

        assertThat(response.getResult()).satisfies(terminal -> {
            assertThat(terminal.getEndReason().getValue()).isEqualTo("SCORE_THRESHOLD");
            assertThat(terminal.getWinners()).singleElement().satisfies(winner -> {
                assertThat(winner.getPlayerId()).isEqualTo(winnerId);
            });
        });
    }

    @Test
    void toResponse_winConditionMetWithMixedWinTypes_omitsResult() {
        var response = toResponse(resultWithTerminal(new TerminalResult(
                GAME_ID,
                "WIN_CONDITION_MET",
                List.of(
                        new TerminalResult.TerminalWinner(UUID.randomUUID(), "WEAVERS", "SCORE_THRESHOLD"),
                        new TerminalResult.TerminalWinner(UUID.randomUUID(), "REVISIONISTS", "FACTION_OBJECTIVE")),
                List.of())));

        assertThat(response.getResult()).isNull();
    }

    private static GetPlayerGameStateUseCase.Result resultWithTerminal(TerminalResult terminal) {
        return new GetPlayerGameStateUseCase.Result(
                GAME_ID,
                2,
                Phase.GAME_ENDED,
                "ERASERS",
                List.of(),
                null,
                List.of(),
                20,
                List.of(),
                List.of(),
                null,
                null,
                null,
                99,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                terminal);
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
