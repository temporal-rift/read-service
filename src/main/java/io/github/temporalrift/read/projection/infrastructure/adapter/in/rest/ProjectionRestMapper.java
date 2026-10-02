package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;

import io.github.temporalrift.read.projection.application.port.in.GetGameHistoryUseCase;
import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.domain.model.DealtCard;
import io.github.temporalrift.read.projection.domain.model.ExposeFact;
import io.github.temporalrift.read.projection.domain.model.ForesightPreview;
import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.model.GameChain;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.LastRoundSummary;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;
import io.github.temporalrift.read.projection.domain.model.PublicBand;
import io.github.temporalrift.read.projection.domain.model.PublicDeclaration;
import io.github.temporalrift.read.projection.domain.model.RevealedHandCardIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedInfluenceIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedIntelEntry;
import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityIntel;
import io.github.temporalrift.read.projection.domain.model.TerminalResult;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ActionFamily;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ActionSummary;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ActiveEvent;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ActivistDeclarationMode;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.CardCategory;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.CardGrade;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.CardType;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.CascadedEvent;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ChainState;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.Deadlines;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.DealtHandCard;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.EligibleResolutionCard;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.EventOutcome;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ExposeSignature;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.Faction;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.FinalScore;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ForesightPreviewEvent;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ForesightPreviewOutcome;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.GameHistoryEra;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.GameHistoryResponse;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.GameResult;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.GameWinner;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.HandCard;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.MySubmission;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PendingHandSelection;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PhaseContext;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PlayerGameStateResponse;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PlayerGameStateResponseMyForesightPreview;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PlayerInGame;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PublicBandEvent;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PublicBandOutcome;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ResolvedOutcome;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.RevealedHandCard;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.RevealedIntel;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.RevealedProbabilityOutcome;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.RoundSummary;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.SpecialAction;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.SubmissionChoice;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.SubmissionProgress;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.SubmissionTargets;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.SubmissionWindow;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.SubmittedCard;

/** Maps projection query results to generated response DTOs. */
final class ProjectionRestMapper {

    private ProjectionRestMapper() {}

    static PlayerGameStateResponse toResponse(GetPlayerGameStateUseCase.Result result, int maxEras) {
        var myRevealedIntel = result.myRevealedIntel().stream()
                .map(ProjectionRestMapper::toRevealedIntel)
                .toList();
        var response = new PlayerGameStateResponse(
                result.gameId(),
                result.eraNumber(),
                result.winScoreThreshold(),
                io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.Phase.valueOf(
                        result.phase().name()),
                result.myHand().stream()
                        .map(card -> toHandCard(card, result.phase(), result.eraNumber(), maxEras))
                        .toList(),
                result.myScore(),
                myRevealedIntel,
                result.activeEvents().stream()
                        .map(ProjectionRestMapper::toActiveEvent)
                        .toList(),
                result.players().stream()
                        .map(ProjectionRestMapper::toPlayerInGame)
                        .toList());
        response.setMyFaction(toFaction(result.myFaction()));
        response.setMySpecialActions(toSpecialActions(result.myFaction()));
        response.setMyJammedUntilRound(result.myJammedUntilRound());
        response.setMyForesightPreview(
                result.myForesightPreview() == null ? null : toForesightPreview(result.myForesightPreview()));
        response.setPendingHandSelection(
                result.pendingHandSelection() == null ? null : toPendingHandSelection(result.pendingHandSelection()));
        response.setLastRoundSummary(
                result.lastRoundSummary() == null ? null : toRoundSummary(result.lastRoundSummary()));
        response.setChain(result.chain() == null ? null : toChainState(result.chain()));
        response.setRevision((int) result.revision());
        response.setLastUpdatedAt(toOffsetDateTime(result.lastUpdatedAt()));
        response.setRoundNumber(result.roundNumber());
        response.setDeadlines(toDeadlines(result));
        response.setPhaseContext(toPhaseContext(result));
        response.setPublicBands(
                result.publicBands().isEmpty()
                        ? null
                        : result.publicBands().stream()
                                .map(ProjectionRestMapper::toPublicBandEvent)
                                .toList());
        response.setDeclarations(
                result.declarations().isEmpty()
                        ? null
                        : result.declarations().stream()
                                .map(ProjectionRestMapper::toPublicDeclaration)
                                .toList());
        response.setExposeFacts(
                result.exposeFacts().isEmpty()
                        ? null
                        : result.exposeFacts().stream()
                                .map(ProjectionRestMapper::toExposeFact)
                                .toList());
        response.setMySubmissions(
                result.mySubmissions().isEmpty()
                        ? null
                        : result.mySubmissions().stream()
                                .map(ProjectionRestMapper::toMySubmission)
                                .toList());
        response.setMyEligibleResolutionCards(
                result.myEligibleResolutionCards() == null
                        ? null
                        : result.myEligibleResolutionCards().stream()
                                .map(card -> new EligibleResolutionCard(
                                        card.cardInstanceId(),
                                        CardType.valueOf(card.cardType()),
                                        toCardGrade(card.grade())))
                                .toList());
        response.setMyEligibleDeclarationModes(
                result.myEligibleDeclarationModes() == null
                        ? null
                        : new LinkedHashSet<>(result.myEligibleDeclarationModes().stream()
                                .map(mode -> ActivistDeclarationMode.valueOf(mode.name()))
                                .toList()));
        // Budgets and objective progress stay absent: no owner fact supplies them (see use-case docs).
        response.setResult(toGameResult(result.terminalResult()));
        return response;
    }

    private static PlayerGameStateResponseMyForesightPreview toForesightPreview(ForesightPreview domain) {
        return new PlayerGameStateResponseMyForesightPreview(
                        domain.nextEraNumber(),
                        domain.revealedEvents().stream()
                                .map(event -> new ForesightPreviewEvent(
                                        event.catalogEventId(),
                                        event.title(),
                                        event.outcomes().stream()
                                                .map(outcome -> new ForesightPreviewOutcome(
                                                        outcome.catalogOutcomeId(),
                                                        outcome.description(),
                                                        outcome.initialProbability()))
                                                .toList()))
                                .toList())
                .emptyReason(domain.emptyReason());
    }

    private static Deadlines toDeadlines(GetPlayerGameStateUseCase.Result result) {
        if (result.handSelectionExpiresAt() == null
                && result.actionRoundExpiresAt() == null
                && result.declarationExpiresAt() == null
                && result.paradoxResolutionExpiresAt() == null) {
            return null;
        }
        return new Deadlines()
                .handSelectionExpiresAt(toOffsetDateTime(result.handSelectionExpiresAt()))
                .actionRoundExpiresAt(toOffsetDateTime(result.actionRoundExpiresAt()))
                .declarationExpiresAt(toOffsetDateTime(result.declarationExpiresAt()))
                .paradoxResolutionExpiresAt(toOffsetDateTime(result.paradoxResolutionExpiresAt()));
    }

    private static PhaseContext toPhaseContext(GetPlayerGameStateUseCase.Result result) {
        var context = new PhaseContext(result.declarationOpen(), result.paradoxOpen());
        if (result.paradoxOpen()) {
            context.setParadoxIds(List.copyOf(result.openParadoxIds()));
            context.setAffectedEventIds(List.copyOf(result.affectedEventIds()));
        }
        if (result.actionRoundProgress() != null) {
            context.setActionRoundProgress(toSubmissionProgress(result.actionRoundProgress()));
        }
        if (result.paradoxResolutionProgress() != null) {
            context.setParadoxResolutionProgress(toSubmissionProgress(result.paradoxResolutionProgress()));
        }
        return context;
    }

    private static SubmissionProgress toSubmissionProgress(
            GetPlayerGameStateUseCase.Result.SubmissionProgress progress) {
        return new SubmissionProgress(
                progress.submittedCount(), progress.totalPlayers(), new LinkedHashSet<>(progress.pendingPlayerIds()));
    }

    private static PublicBandEvent toPublicBandEvent(PublicBand domain) {
        return new PublicBandEvent(
                domain.eventId(),
                domain.observedInRound(),
                domain.outcomes().stream()
                        .map(outcome -> new PublicBandOutcome(
                                outcome.outcomeId(), PublicBandOutcome.BandEnum.fromValue(outcome.band())))
                        .toList());
    }

    private static io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PublicDeclaration
            toPublicDeclaration(PublicDeclaration domain) {
        return new io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PublicDeclaration(
                domain.playerId(),
                io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PublicDeclaration
                        .ModeEnum.fromValue(domain.mode()),
                domain.targetEventId(),
                domain.targetOutcomeId(),
                domain.eraNumber());
    }

    private static io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ExposeFact
            toExposeFact(ExposeFact domain) {
        var response = new io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ExposeFact(
                domain.activistPlayerId(), domain.targetPlayerId(), domain.roundNumber(), domain.behaviorChanged());
        if (domain.signatureType() != null) {
            response.setSignature(new ExposeSignature(
                            ExposeSignature.TypeEnum.fromValue(domain.signatureType()), domain.signatureTargetEventId())
                    .sourceOutcomeId(domain.signatureSourceOutcomeId())
                    .targetOutcomeId(domain.signatureTargetOutcomeId()));
        }
        return response;
    }

    private static MySubmission toMySubmission(PlayerSubmission domain) {
        var response = new MySubmission(
                domain.eraNumber(), SubmissionWindow.valueOf(domain.window().name()), MySubmission.StatusEnum.ACCEPTED);
        response.setRoundNumber(domain.roundNumber());
        if (domain.choice() != null) {
            response.setChoice(SubmissionChoice.valueOf(domain.choice().name()));
        }
        if (domain.card() != null) {
            var card = domain.card();
            response.setCard(new SubmittedCard(
                            card.cardInstanceId(),
                            io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.CardType
                                    .valueOf(card.cardType()),
                            toCardGrade(card.grade()))
                    .disguiseCategory(
                            card.disguiseCategory() == null ? null : CardCategory.valueOf(card.disguiseCategory())));
        }
        if (domain.specialAction() != null) {
            response.setSpecialAction(SpecialAction.valueOf(domain.specialAction()));
        }
        if (domain.targets() != null) {
            var targets = domain.targets();
            response.setTargets(new SubmissionTargets()
                    .targetEventId(targets.targetEventId())
                    .targetEventIds(
                            targets.targetEventIds() == null ? null : new LinkedHashSet<>(targets.targetEventIds()))
                    .targetOutcomeId(targets.targetOutcomeId())
                    .sourceEventId(targets.sourceEventId())
                    .sourceOutcomeId(targets.sourceOutcomeId())
                    .targetPlayerId(targets.targetPlayerId())
                    .targetPlayerIds(
                            targets.targetPlayerIds() == null ? null : new LinkedHashSet<>(targets.targetPlayerIds())));
        }
        return response;
    }

    /**
     * Terminal facts are exposed only for ended games (the handler already nulls them otherwise).
     * An owner end reason outside the contract omits the whole result rather than fabricating one.
     */
    private static GameResult toGameResult(TerminalResult domain) {
        if (domain == null) {
            return null;
        }
        var endReason = toEndReason(domain.endReason());
        if (endReason == null) {
            return null;
        }
        return new GameResult(
                endReason,
                domain.winners().stream()
                        .map(winner -> new GameWinner(winner.playerId())
                                .faction(toFaction(winner.faction()))
                                .winType(toWinType(winner.winType())))
                        .toList(),
                domain.finalScores().stream()
                        .map(score -> new FinalScore(score.playerId(), score.score()))
                        .toList(),
                GameResult.RevealBoundaryEnum.FACTIONS_AND_SCORES_PUBLIC);
    }

    private static GameResult.EndReasonEnum toEndReason(String reason) {
        try {
            return GameResult.EndReasonEnum.fromValue(reason);
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    private static GameWinner.WinTypeEnum toWinType(String winType) {
        if (winType == null) {
            return null;
        }
        try {
            return GameWinner.WinTypeEnum.fromValue(winType);
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    private static OffsetDateTime toOffsetDateTime(java.time.Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static ChainState toChainState(GameChain domain) {
        return new ChainState(ChainState.StatusEnum.valueOf(domain.status().name()), domain.length());
    }

    private static RoundSummary toRoundSummary(LastRoundSummary summary) {
        return new RoundSummary(
                summary.roundNumber(),
                summary.actionSummaries().stream()
                        .map(action -> new ActionSummary(action.playerId(), action.skipped())
                                .actionCategory(toCardCategory(action.actionCategory()))
                                .actionFamily(toActionFamily(action.actionFamily())))
                        .toList());
    }

    private static CardCategory toCardCategory(String category) {
        return category == null || category.equals("UNKNOWN") ? null : CardCategory.fromValue(category);
    }

    private static ActionFamily toActionFamily(String family) {
        return family == null || family.equals("UNKNOWN") ? null : ActionFamily.fromValue(family);
    }

    private static RevealedIntel toRevealedIntel(RevealedIntelEntry domain) {
        return switch (domain) {
            case RevealedProbabilityIntel probability -> toProbabilityIntel(probability);
            case RevealedInfluenceIntel influence -> toInfluenceIntel(influence);
            case RevealedHandCardIntel handCard -> toHandCardIntel(handCard);
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
        response.setMimicInfluencerPlayerIds(new LinkedHashSet<>(domain.mimicInfluencerPlayerIds()));
        return response;
    }

    private static RevealedIntel toHandCardIntel(RevealedHandCardIntel domain) {
        var response = new RevealedIntel(RevealedIntel.KindEnum.HAND_CARD, domain.observedInRound(), domain.eventId());
        response.setTargetPlayerId(domain.targetPlayerId());
        response.setRevealedCards(domain.revealedCards().stream()
                .map(card -> new RevealedHandCard(
                        card.cardInstanceId(), toCardType(card.cardType()), toCardGrade(card.grade())))
                .toList());
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
                domain.cardInstanceId(), toCardType(domain.cardType()), toCardGrade(domain.grade()), domain.dealSlot());
    }

    private static HandCard toHandCard(
            io.github.temporalrift.read.projection.domain.model.HandCard domain,
            Phase phase,
            int eraNumber,
            int maxEras) {
        return new HandCard(
                domain.cardInstanceId(),
                toCardType(domain.cardType()),
                toCardGrade(domain.grade()),
                isPlayableThisRound(domain.cardType(), phase, eraNumber, maxEras));
    }

    private static boolean isPlayableThisRound(String cardType, Phase phase, int eraNumber, int maxEras) {
        return switch (cardType) {
            case "TRACE" -> !(phase == Phase.ACTION_ROUND_1 && eraNumber == 1);
            case "JAM", "SCAN", "INTERCEPT" -> phase != Phase.ACTION_ROUND_3;
            case "STALL" -> eraNumber < maxEras;
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
    private static List<SpecialAction> toSpecialActions(String faction) {
        return switch (faction) {
            case "ERASERS" -> List.of(SpecialAction.ANNIHILATE, SpecialAction.CORRUPT, SpecialAction.CASCADE);
            case "PROPHETS" -> List.of(SpecialAction.FORESIGHT, SpecialAction.SEAL, SpecialAction.FULFILLMENT);
            case "REVISIONISTS" -> List.of(SpecialAction.REWRITE, SpecialAction.MIMIC, SpecialAction.OBSCURE);
            case "WEAVERS" -> List.of(SpecialAction.THREAD, SpecialAction.TAPESTRY, SpecialAction.REWEAVE);
            case "ACTIVISTS" -> List.of(SpecialAction.RALLY, SpecialAction.EXPOSE, SpecialAction.MOMENTUM);
            case null, default -> List.of();
        };
    }

    private static PendingHandSelection toPendingHandSelection(
            io.github.temporalrift.read.projection.domain.model.PendingHandSelection domain) {
        var cards = domain.cards().stream()
                .map(card -> new DealtHandCard(
                        card.cardInstanceId(), toCardType(card.cardType()), toCardGrade(card.grade()), card.dealSlot()))
                .toList();
        return new PendingHandSelection(
                cards,
                PendingHandSelection.RequiredSelectionCountEnum.NUMBER_5,
                OffsetDateTime.ofInstant(domain.expiresAt(), ZoneOffset.UTC));
    }

    private static CardGrade toCardGrade(String grade) {
        return CardGrade.valueOf(grade);
    }

    private static CardType toCardType(String cardType) {
        return CardType.valueOf(cardType);
    }

    private static Faction toFaction(String faction) {
        return faction == null ? null : Faction.valueOf(faction);
    }

    private static ActiveEvent toActiveEvent(GameActiveEvent domain) {
        var outcomes = domain.outcomes().stream()
                .map(o -> new EventOutcome(o.outcomeId(), o.description(), o.initialProbability()))
                .toList();
        return new ActiveEvent(
                domain.eventId(),
                domain.title(),
                ActiveEvent.CarryOverStateEnum.valueOf(domain.carryOverState().name()),
                outcomes);
    }

    private static PlayerInGame toPlayerInGame(GamePlayer domain) {
        var playerInGame = new PlayerInGame(domain.playerId(), domain.score(), domain.isConnected());
        playerInGame.setFaction(toFaction(domain.faction()));
        playerInGame.setPlayerName(domain.playerName());
        return playerInGame;
    }
}
