package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import io.github.temporalrift.read.projection.application.port.in.GetGameHistoryUseCase;
import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.domain.model.DealtCard;
import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.LastRoundSummary;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.RevealedInfluenceIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedIntelEntry;
import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityIntel;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ActionSummary;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ActiveEvent;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.CardGrade;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.CascadedEvent;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.DealtHandCard;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.EventOutcome;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.GameHistoryEra;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.GameHistoryResponse;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.HandCard;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PendingHandSelection;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PlayerGameStateResponse;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PlayerInGame;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ResolvedOutcome;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.RevealedIntel;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.RevealedProbabilityOutcome;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.RoundSummary;

/** Maps projection query results to generated response DTOs. */
final class ProjectionRestMapper {

    private ProjectionRestMapper() {}

    static PlayerGameStateResponse toResponse(GetPlayerGameStateUseCase.Result result) {
        var myRevealedIntel = result.myRevealedIntel().stream()
                .map(ProjectionRestMapper::toRevealedIntel)
                .toList();
        var response = new PlayerGameStateResponse(
                result.gameId(),
                result.eraNumber(),
                io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.Phase.valueOf(
                        result.phase().name()),
                result.myHand().stream()
                        .map(card -> toHandCard(card, result.phase(), result.eraNumber()))
                        .toList(),
                result.myScore(),
                myRevealedIntel,
                result.activeEvents().stream()
                        .map(ProjectionRestMapper::toActiveEvent)
                        .toList(),
                result.players().stream()
                        .map(ProjectionRestMapper::toPlayerInGame)
                        .toList());
        response.setMyFaction(result.myFaction());
        response.setMySpecialActions(toSpecialActions(result.myFaction()));
        response.setMyJammedUntilRound(result.myJammedUntilRound());
        response.setPendingHandSelection(
                result.pendingHandSelection() == null ? null : toPendingHandSelection(result.pendingHandSelection()));
        response.setLastRoundSummary(
                result.lastRoundSummary() == null ? null : toRoundSummary(result.lastRoundSummary()));
        return response;
    }

    private static RoundSummary toRoundSummary(LastRoundSummary summary) {
        return new RoundSummary(
                summary.roundNumber(),
                summary.actionSummaries().stream()
                        .map(action -> new ActionSummary(action.playerId(), action.skipped())
                                .actionCategory(action.actionCategory())
                                .actionFamily(action.actionFamily()))
                        .toList());
    }

    private static RevealedIntel toRevealedIntel(RevealedIntelEntry domain) {
        return switch (domain) {
            case RevealedProbabilityIntel probability -> toProbabilityIntel(probability);
            case RevealedInfluenceIntel influence -> toInfluenceIntel(influence);
        };
    }

    private static RevealedIntel toProbabilityIntel(RevealedProbabilityIntel domain) {
        var response =
                new RevealedIntel(RevealedIntel.KindEnum.PROBABILITY, domain.observedInRound(), domain.eventId());
        response.setOutcomes(domain.outcomes().stream()
                .map(outcome -> new RevealedProbabilityOutcome(
                        outcome.outcomeId(), outcome.probability(), outcome.isAnnihilated(), outcome.isSealed()))
                .toList());
        return response;
    }

    private static RevealedIntel toInfluenceIntel(RevealedInfluenceIntel domain) {
        var response = new RevealedIntel(RevealedIntel.KindEnum.INFLUENCE, domain.observedInRound(), domain.eventId());
        response.setInfluencerPlayerIds(List.copyOf(domain.influencerPlayerIds()));
        return response;
    }

    static GameHistoryResponse toResponse(GetGameHistoryUseCase.Result result) {
        return new GameHistoryResponse(
                result.gameId(),
                result.eras().stream().map(ProjectionRestMapper::toHistoryEra).toList());
    }

    private static GameHistoryEra toHistoryEra(GetGameHistoryUseCase.EraResult era) {
        var response = new GameHistoryEra(
                era.eraNumber(),
                era.outcomes().stream()
                        .map(outcome -> new ResolvedOutcome(
                                outcome.eventId(),
                                outcome.title(),
                                outcome.winningOutcomeId(),
                                outcome.winningOutcomeDescription()))
                        .toList(),
                era.paradoxesCascaded(),
                era.myHand().stream().map(ProjectionRestMapper::toDealtHandCard).toList());
        response.setCascadedEvents(
                era.cascadedEvents().isEmpty()
                        ? null
                        : era.cascadedEvents().stream()
                                .map(event -> new CascadedEvent(event.eventId(), event.title()))
                                .toList());
        return response;
    }

    private static DealtHandCard toDealtHandCard(DealtCard domain) {
        return new DealtHandCard(
                domain.cardInstanceId(), domain.cardType(), toCardGrade(domain.grade()), domain.dealSlot());
    }

    private static HandCard toHandCard(
            io.github.temporalrift.read.projection.domain.model.HandCard domain, Phase phase, int eraNumber) {
        return new HandCard(
                domain.cardInstanceId(),
                domain.cardType(),
                toCardGrade(domain.grade()),
                isPlayableThisRound(domain.cardType(), phase, eraNumber));
    }

    private static boolean isPlayableThisRound(String cardType, Phase phase, int eraNumber) {
        return switch (cardType) {
            case "TRACE" -> !(phase == Phase.ACTION_ROUND_1 && eraNumber == 1);
            case "JAM", "SCAN", "INTERCEPT" -> phase != Phase.ACTION_ROUND_3;
            case "STABILIZE", "DETONATE" -> !isActionRound(phase);
            default -> true;
        };
    }

    private static boolean isActionRound(Phase phase) {
        return phase == Phase.ACTION_ROUND_1 || phase == Phase.ACTION_ROUND_2 || phase == Phase.ACTION_ROUND_3;
    }

    /**
     * Each faction's fixed set of three special actions, empty before a faction is assigned. Static only —
     * whether a special is currently usable (once-per-era budgets, timing windows, jam) is a game-service rule
     * this projection does not represent.
     */
    private static List<String> toSpecialActions(String faction) {
        return switch (faction) {
            case "ERASERS" -> List.of("ANNIHILATE", "CORRUPT", "CASCADE");
            case "PROPHETS" -> List.of("FORESIGHT", "SEAL", "FULFILLMENT");
            case "REVISIONISTS" -> List.of("REWRITE", "MIMIC", "OBSCURE");
            case "WEAVERS" -> List.of("THREAD", "TAPESTRY", "UNRAVEL");
            case "ACTIVISTS" -> List.of("RALLY", "EXPOSE", "MOMENTUM");
            case null, default -> List.of();
        };
    }

    private static PendingHandSelection toPendingHandSelection(
            io.github.temporalrift.read.projection.domain.model.PendingHandSelection domain) {
        var cards = domain.cards().stream()
                .map(card -> new DealtHandCard(
                        card.cardInstanceId(), card.cardType(), toCardGrade(card.grade()), card.dealSlot()))
                .toList();
        return new PendingHandSelection(
                cards,
                PendingHandSelection.RequiredSelectionCountEnum.NUMBER_5,
                OffsetDateTime.ofInstant(domain.expiresAt(), ZoneOffset.UTC));
    }

    private static CardGrade toCardGrade(String grade) {
        return CardGrade.valueOf(grade);
    }

    private static ActiveEvent toActiveEvent(GameActiveEvent domain) {
        var outcomes = domain.outcomes().stream()
                .map(o -> new EventOutcome(o.outcomeId(), o.description()))
                .toList();
        return new ActiveEvent(
                domain.eventId(),
                domain.title(),
                ActiveEvent.CarryOverStateEnum.valueOf(domain.carryOverState().name()),
                outcomes);
    }

    private static PlayerInGame toPlayerInGame(GamePlayer domain) {
        var playerInGame = new PlayerInGame(domain.playerId(), domain.score(), domain.isConnected());
        playerInGame.setFaction(domain.faction());
        return playerInGame;
    }
}
