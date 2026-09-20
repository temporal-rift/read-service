package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActionRoundStartedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActivistDeclarationRecordedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.BandedProbabilityPublishedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardPlayedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ExposeBehaviorChangedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ExposeSignatureRevealedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.HandCardInterceptedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.InfluenceTracedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ParadoxResolutionCardPlayedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.PlayerJammedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.RoundSummaryPublishedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.SpecialActionPlayedPayload;
import io.github.temporalrift.asyncapi.scoringevents.GeneratedChannelContract.ScoresUpdatedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EraEndedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EraStartedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventsDrawnPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.FactionAssignedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.FactionRevealedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GameEndedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GameStartedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandDealtPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandSelectedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PlayerAbandonedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PlayerDisconnectedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.ResolutionStartedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.TimelineCollapsedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.TimelineStabilizedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.WinConditionMetPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.AdjustedBandsPublishedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainBrokenPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainCompletedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainLinkAddedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.OutcomeAppliedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxCascadedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxResolutionPhaseStartedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxResolvedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityStateRevealedPayload;
import io.github.temporalrift.read.projection.domain.model.CarryOverState;
import io.github.temporalrift.read.projection.domain.model.ChainStatus;
import io.github.temporalrift.read.projection.domain.model.EventOutcome;
import io.github.temporalrift.read.projection.domain.model.ExposeFact;
import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.model.GameChain;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.GameProjection;
import io.github.temporalrift.read.projection.domain.model.HandCard;
import io.github.temporalrift.read.projection.domain.model.LastRoundSummary;
import io.github.temporalrift.read.projection.domain.model.PendingHandCard;
import io.github.temporalrift.read.projection.domain.model.PendingHandSelection;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.PlayerGameState;
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
import io.github.temporalrift.read.projection.domain.port.out.ExposeFactRepository;
import io.github.temporalrift.read.projection.domain.port.out.GameActiveEventRepository;
import io.github.temporalrift.read.projection.domain.port.out.GameChainRepository;
import io.github.temporalrift.read.projection.domain.port.out.GamePlayerRepository;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;
import io.github.temporalrift.read.projection.domain.port.out.PlayerGameStateRepository;
import io.github.temporalrift.read.projection.domain.port.out.PlayerSubmissionRepository;
import io.github.temporalrift.read.projection.domain.port.out.PublicBandRepository;
import io.github.temporalrift.read.projection.domain.port.out.PublicDeclarationRepository;
import io.github.temporalrift.read.projection.domain.port.out.RevealedHandCardIntelRepository;
import io.github.temporalrift.read.projection.domain.port.out.RevealedInfluenceIntelRepository;
import io.github.temporalrift.read.projection.domain.port.out.RevealedProbabilityIntelRepository;
import io.github.temporalrift.read.projection.domain.port.out.TerminalResultRepository;

/**
 * Applies each consumed event to the read models per design.md Decisions 5–7. Package-scoped to
 * {@code infrastructure.adapter.in.kafka} because it operates directly on the wire-shaped payload records —
 * matching {@code timeline-service}'s precedent of translating payload to domain-port calls inline in the
 * consuming layer, not through a separate application-layer command handler.
 */
@Component
class ProjectionEventApplier {

    private static final Logger log = LoggerFactory.getLogger(ProjectionEventApplier.class);

    private final GameProjectionRepository gameProjections;
    private final GamePlayerRepository gamePlayers;
    private final GameActiveEventRepository gameActiveEvents;
    private final PlayerGameStateRepository playerGameStates;
    private final RevealedProbabilityIntelRepository revealedProbabilityIntel;
    private final RevealedInfluenceIntelRepository revealedInfluenceIntel;
    private final RevealedHandCardIntelRepository revealedHandCardIntel;
    private final GameChainRepository gameChains;
    private final PublicBandRepository publicBands;
    private final PublicDeclarationRepository publicDeclarations;
    private final ExposeFactRepository exposeFacts;
    private final PlayerSubmissionRepository playerSubmissions;
    private final TerminalResultRepository terminalResults;

    ProjectionEventApplier(
            GameProjectionRepository gameProjections,
            GamePlayerRepository gamePlayers,
            GameActiveEventRepository gameActiveEvents,
            PlayerGameStateRepository playerGameStates,
            RevealedProbabilityIntelRepository revealedProbabilityIntel,
            RevealedInfluenceIntelRepository revealedInfluenceIntel,
            RevealedHandCardIntelRepository revealedHandCardIntel,
            GameChainRepository gameChains,
            PublicBandRepository publicBands,
            PublicDeclarationRepository publicDeclarations,
            ExposeFactRepository exposeFacts,
            PlayerSubmissionRepository playerSubmissions,
            TerminalResultRepository terminalResults) {
        this.gameProjections = gameProjections;
        this.gamePlayers = gamePlayers;
        this.gameActiveEvents = gameActiveEvents;
        this.playerGameStates = playerGameStates;
        this.revealedProbabilityIntel = revealedProbabilityIntel;
        this.revealedInfluenceIntel = revealedInfluenceIntel;
        this.revealedHandCardIntel = revealedHandCardIntel;
        this.gameChains = gameChains;
        this.publicBands = publicBands;
        this.publicDeclarations = publicDeclarations;
        this.exposeFacts = exposeFacts;
        this.playerSubmissions = playerSubmissions;
        this.terminalResults = terminalResults;
    }

    // Preserves rather than overwrites: a per-player event can arrive, and find-or-create a row,
    // before GameStarted does (see applyFactionAssigned). Re-initializing unconditionally here would
    // stomp that already-correct data back to defaults the instant GameStarted is finally applied,
    // silently undoing the out-of-order handling below rather than complementing it.
    void applyGameStarted(GameStartedPayload payload) {
        lockGame(payload.gameId());
        for (var playerId : payload.playerIds()) {
            gamePlayers.save(payload.gameId(), findOrCreateGamePlayer(payload.gameId(), playerId));
            playerGameStates.save(findOrCreatePlayerGameState(payload.gameId(), playerId));
        }
    }

    // Per-player events are not guaranteed to arrive after GameStarted creates the row they target —
    // IncompleteEventPublicationResubmitter can resubmit a failed send out of original order, so
    // delivery order is not a safe assumption. Find-or-create rather than skip-on-missing, matching
    // game-service's own ActionStateProjectionEventListener.onHandDealt precedent for the same race.
    void applyFactionAssigned(FactionAssignedPayload payload) {
        var existing = findOrCreatePlayerGameState(payload.gameId(), payload.playerId());
        playerGameStates.save(new PlayerGameState(
                existing.gameId(),
                existing.playerId(),
                payload.faction().name(),
                existing.myHand(),
                existing.pendingHandSelection(),
                existing.jammedEraNumber(),
                existing.jammedUntilRound()));
    }

    // The suppressed player's private reveal (design.md "Store the raw jam fact; compute 'is it still active'
    // at read time") — find-or-create matches applyFactionAssigned's precedent for the same out-of-order race.
    // Eras never move backward within a game, so a payload older than the last-recorded jam era can only be a
    // delayed resubmission of a stale event; applying it would wrongly revert an already-superseded jam.
    void applyPlayerJammed(PlayerJammedPayload payload) {
        var existing = findOrCreatePlayerGameState(payload.gameId(), payload.playerId());
        if (existing.jammedEraNumber() != null && payload.eraNumber() < existing.jammedEraNumber()) {
            log.warn("PlayerJammed for past era {} in game {} — skipping", payload.eraNumber(), payload.gameId());
            return;
        }
        playerGameStates.save(new PlayerGameState(
                existing.gameId(),
                existing.playerId(),
                existing.myFaction(),
                existing.myHand(),
                existing.pendingHandSelection(),
                payload.eraNumber(),
                payload.jammedUntilRound()));
    }

    void applyEraStarted(EraStartedPayload payload) {
        var existing = lockGame(payload.gameId());
        if (isSupersededOrGameEnded(payload.eraNumber(), existing)
                || regressesPhaseWithinEra(existing, payload.eraNumber(), Phase.ERA_START)) {
            log.warn(
                    "EraStarted for past/ended/already-started era {} in game {} — skipping",
                    payload.eraNumber(),
                    payload.gameId());
            return;
        }
        gameProjections.save(new GameProjection(
                payload.gameId(), payload.eraNumber(), Phase.ERA_START, List.of(), existing.lastRoundSummary()));
    }

    void applyEventsDrawn(EventsDrawnPayload payload) {
        var existing = lockGame(payload.gameId());
        if (isSupersededOrGameEnded(payload.eraNumber(), existing)) {
            log.warn("EventsDrawn for past/ended era {} in game {} — skipping", payload.eraNumber(), payload.gameId());
            return;
        }
        if (payload.eraNumber() > existing.eraNumber()) {
            gameProjections.save(new GameProjection(
                    payload.gameId(), payload.eraNumber(), Phase.ERA_START, List.of(), existing.lastRoundSummary()));
        }
        for (var event : payload.events()) {
            if (gameActiveEvents.isResolved(payload.gameId(), event.eventId())) {
                continue;
            }
            var outcomes = event.outcomes().stream()
                    .map(o -> new EventOutcome(o.outcomeId(), o.description()))
                    .toList();
            gameActiveEvents.save(
                    payload.gameId(),
                    new GameActiveEvent(
                            event.eventId(),
                            event.title(),
                            CarryOverState.valueOf(event.carryOverState().name()),
                            outcomes));
        }
    }

    // HandDealt holds the unresolved pool. The last final hand remains playable until HandSelected arrives.
    void applyHandDealt(HandDealtPayload payload) {
        var existing = findOrCreatePlayerGameState(payload.gameId(), payload.playerId());
        var pendingCards = payload.cards().stream()
                .map(card -> new PendingHandCard(
                        card.cardInstanceId(),
                        card.cardType().name(),
                        card.grade().name(),
                        card.dealSlot()))
                .toList();
        playerGameStates.save(new PlayerGameState(
                existing.gameId(),
                existing.playerId(),
                existing.myFaction(),
                existing.myHand(),
                new PendingHandSelection(pendingCards, payload.selectionExpiresAt()),
                existing.jammedEraNumber(),
                existing.jammedUntilRound()));
    }

    void applyHandSelected(HandSelectedPayload payload) {
        var existing = findOrCreatePlayerGameState(payload.gameId(), payload.playerId());
        var hand = payload.cards().stream()
                .map(card -> new HandCard(
                        card.cardInstanceId(),
                        card.cardType().name(),
                        card.grade().name()))
                .toList();
        playerGameStates.save(new PlayerGameState(
                existing.gameId(),
                existing.playerId(),
                existing.myFaction(),
                hand,
                null,
                existing.jammedEraNumber(),
                existing.jammedUntilRound()));
        playerSubmissions.upsert(new PlayerSubmission(
                payload.gameId(),
                payload.playerId(),
                payload.eraNumber(),
                null,
                PlayerSubmission.SubmissionKind.HAND_SELECTION,
                null));
    }

    private PlayerGameState findOrCreatePlayerGameState(UUID gameId, UUID playerId) {
        return playerGameStates
                .findByGameIdAndPlayerId(gameId, playerId)
                .orElseGet(() -> new PlayerGameState(gameId, playerId, null, List.of()));
    }

    void applyPlayerDisconnected(PlayerDisconnectedPayload payload) {
        setConnected(payload.gameId(), payload.playerId(), false);
    }

    void applyPlayerAbandoned(PlayerAbandonedPayload payload) {
        setConnected(payload.gameId(), payload.playerId(), false);
    }

    private void setConnected(UUID gameId, UUID playerId, boolean connected) {
        var existing = findOrCreateGamePlayer(gameId, playerId);
        gamePlayers.save(gameId, new GamePlayer(existing.playerId(), existing.score(), connected, existing.faction()));
    }

    private GamePlayer findOrCreateGamePlayer(UUID gameId, UUID playerId) {
        return gamePlayers
                .findByGameIdAndPlayerId(gameId, playerId)
                .orElseGet(() -> new GamePlayer(playerId, 0, true, null));
    }

    void applyEraEnded(EraEndedPayload payload) {
        var existing = lockGame(payload.gameId());
        if (isSupersededOrGameEnded(payload.eraNumber(), existing)) {
            log.warn("EraEnded for past/ended era {} in game {} — skipping", payload.eraNumber(), payload.gameId());
            return;
        }
        gameProjections.save(new GameProjection(
                payload.gameId(), payload.eraNumber(), Phase.ERA_END, List.of(), existing.lastRoundSummary()));
        revealedProbabilityIntel.deleteByGameIdAndEraNumber(payload.gameId(), payload.eraNumber());
        revealedInfluenceIntel.deleteByGameIdAndEraNumber(payload.gameId(), payload.eraNumber());
        revealedHandCardIntel.deleteByGameIdAndEraNumber(payload.gameId(), payload.eraNumber());
        clearRecoverableEraState(payload.gameId(), payload.eraNumber());
        // Defensive clear — design.md Decision 6. Every drawn event currently gets an OutcomeApplied (no
        // cascade/paradox handling exists yet), so this is normally a no-op.
        gameActiveEvents.deleteByGameId(payload.gameId());
    }

    /** Era-scoped recoverable rows never survive their era — a delayed fact must not repopulate them. */
    private void clearRecoverableEraState(UUID gameId, int eraNumber) {
        publicBands.deleteByGameIdAndEraNumber(gameId, eraNumber);
        publicDeclarations.deleteByGameIdAndEraNumber(gameId, eraNumber);
        exposeFacts.deleteByGameIdAndEraNumber(gameId, eraNumber);
        playerSubmissions.deleteByGameIdAndEraNumber(gameId, eraNumber);
    }

    // A winner can end the game directly from the final era without an intervening EraEnded for that
    // era, so this era's scan intel and active events would otherwise never be cleared. The intel
    // clear is game-scoped, not current-era-scoped: an out-of-order future-era reveal can be
    // persisted while the projection still lags on an earlier era, and that row must not orphan.
    void applyGameEnded(GameEndedPayload payload) {
        var existing = lockGame(payload.gameId());
        if (existing.phase() == Phase.GAME_ENDED) {
            log.warn("GameEnded already applied for game {} — skipping", payload.gameId());
            return;
        }
        var eraNumber = existing.eraNumber();
        gameProjections.save(new GameProjection(
                payload.gameId(), eraNumber, Phase.GAME_ENDED, List.of(), existing.lastRoundSummary()));
        revealedProbabilityIntel.deleteByGameId(payload.gameId());
        revealedInfluenceIntel.deleteByGameId(payload.gameId());
        revealedHandCardIntel.deleteByGameId(payload.gameId());
        publicBands.deleteByGameId(payload.gameId());
        publicDeclarations.deleteByGameId(payload.gameId());
        exposeFacts.deleteByGameId(payload.gameId());
        playerSubmissions.deleteByGameId(payload.gameId());
        gameActiveEvents.deleteByGameId(payload.gameId());
        gameChains.deleteByGameId(payload.gameId());
        terminalResults.saveEndReasonAndScores(
                payload.gameId(),
                payload.endReason(),
                payload.finalScores().stream()
                        .map(score -> new TerminalResult.TerminalScore(
                                score.playerId(), score.faction().name(), score.score()))
                        .toList());
        for (var finalScore : payload.finalScores()) {
            gamePlayers
                    .findByGameIdAndPlayerId(payload.gameId(), finalScore.playerId())
                    .ifPresent(existingPlayer -> gamePlayers.save(
                            payload.gameId(),
                            new GamePlayer(
                                    existingPlayer.playerId(),
                                    finalScore.score(),
                                    existingPlayer.isConnected(),
                                    existingPlayer.faction())));
        }
    }

    void applyFactionRevealed(FactionRevealedPayload payload) {
        for (var reveal : payload.reveals()) {
            gamePlayers
                    .findByGameIdAndPlayerId(payload.gameId(), reveal.playerId())
                    .ifPresent(existing -> gamePlayers.save(
                            payload.gameId(),
                            new GamePlayer(
                                    existing.playerId(),
                                    existing.score(),
                                    existing.isConnected(),
                                    reveal.faction().name())));
        }
    }

    void applyResolutionStarted(ResolutionStartedPayload payload) {
        var existing = lockGame(payload.gameId());
        if (isSupersededOrGameEnded(payload.eraNumber(), existing)
                || regressesPhaseWithinEra(existing, payload.eraNumber(), Phase.RESOLUTION)) {
            log.warn(
                    "ResolutionStarted for past/ended/already-resolved era {} in game {} — skipping",
                    payload.eraNumber(),
                    payload.gameId());
            return;
        }
        gameProjections.save(new GameProjection(
                payload.gameId(),
                payload.eraNumber(),
                Phase.RESOLUTION,
                existing.pendingParadoxIds(),
                existing.lastRoundSummary(),
                null,
                null,
                null,
                existing.revision(),
                existing.lastUpdatedAt()));
    }

    void applyActionRoundStarted(ActionRoundStartedPayload payload, Instant occurredAt) {
        var phase =
                switch (payload.roundNumber()) {
                    case 1 -> Phase.ACTION_ROUND_1;
                    case 2 -> Phase.ACTION_ROUND_2;
                    case 3 -> Phase.ACTION_ROUND_3;
                    default -> throw new IllegalArgumentException("Unsupported roundNumber " + payload.roundNumber());
                };
        var existing = lockGame(payload.gameId());
        if (isSupersededOrGameEnded(payload.eraNumber(), existing)
                || regressesPhaseWithinEra(existing, payload.eraNumber(), phase)) {
            log.warn(
                    "ActionRoundStarted for past/ended/already-passed era {} in game {} — skipping",
                    payload.eraNumber(),
                    payload.gameId());
            return;
        }
        // The round timer is an authoritative owner fact: expiry is opening time plus allotted seconds.
        var expiresAt = occurredAt == null ? null : occurredAt.plusSeconds(payload.timerSeconds());
        gameProjections.save(new GameProjection(
                payload.gameId(),
                payload.eraNumber(),
                phase,
                existing.pendingParadoxIds(),
                existing.lastRoundSummary(),
                payload.roundNumber(),
                expiresAt,
                null,
                existing.revision(),
                existing.lastUpdatedAt()));
    }

    void applyCardPlayed(CardPlayedPayload payload) {
        // The submission is the player's own accepted decision: it is recorded even when the
        // player-state row does not exist yet (out-of-order delivery), while hand removal still
        // needs the row. Both are scoped to the caller's era, so a later GameStarted cannot leak them.
        playerSubmissions.upsert(new PlayerSubmission(
                payload.gameId(),
                payload.playerId(),
                payload.eraNumber(),
                payload.roundNumber(),
                PlayerSubmission.SubmissionKind.ACTION,
                "CARD"));
        playerGameStates
                .findByGameIdAndPlayerId(payload.gameId(), payload.playerId())
                .ifPresentOrElse(
                        existing -> {
                            var hand = existing.myHand().stream()
                                    .filter(card -> !card.cardInstanceId().equals(payload.cardInstanceId()))
                                    .toList();
                            playerGameStates.save(new PlayerGameState(
                                    existing.gameId(),
                                    existing.playerId(),
                                    existing.myFaction(),
                                    hand,
                                    existing.pendingHandSelection(),
                                    existing.jammedEraNumber(),
                                    existing.jammedUntilRound()));
                        },
                        () -> log.warn(
                                "CardPlayed for unknown player {} in game {} — skipping hand removal",
                                payload.playerId(),
                                payload.gameId()));
    }

    void applySpecialActionPlayed(SpecialActionPlayedPayload payload) {
        if (isStaleEra(payload.gameId(), payload.eraNumber(), "SpecialActionPlayed")) {
            return;
        }
        playerSubmissions.upsert(new PlayerSubmission(
                payload.gameId(),
                payload.playerId(),
                payload.eraNumber(),
                payload.roundNumber(),
                PlayerSubmission.SubmissionKind.ACTION,
                "SPECIAL"));
        touchRevision(payload.gameId());
    }

    void applyParadoxResolutionCardPlayed(ParadoxResolutionCardPlayedPayload payload) {
        if (isStaleEra(payload.gameId(), payload.eraNumber(), "ParadoxResolutionCardPlayed")) {
            return;
        }
        playerSubmissions.upsert(new PlayerSubmission(
                payload.gameId(),
                payload.playerId(),
                payload.eraNumber(),
                null,
                PlayerSubmission.SubmissionKind.PARADOX_CARD,
                null));
        touchRevision(payload.gameId());
    }

    void applyRoundSummaryPublished(RoundSummaryPublishedPayload payload) {
        var existing = lockGame(payload.gameId());
        if (isSupersededOrGameEnded(payload.eraNumber(), existing)
                || isStaleRoundSummary(payload, existing.lastRoundSummary())) {
            log.warn(
                    "RoundSummaryPublished for stale or ended era {} round {} in game {} — skipping",
                    payload.eraNumber(),
                    payload.roundNumber(),
                    payload.gameId());
            return;
        }
        var summary = new LastRoundSummary(
                payload.eraNumber(),
                payload.roundNumber(),
                payload.actionSummaries().stream()
                        .map(action -> new RoundActionSummary(
                                action.playerId(), action.actionCategory(), action.actionFamily(), action.skipped()))
                        .toList());
        gameProjections.save(new GameProjection(
                existing.gameId(),
                existing.eraNumber(),
                existing.phase(),
                existing.pendingParadoxIds(),
                summary,
                existing.currentRoundNumber(),
                existing.actionRoundExpiresAt(),
                existing.paradoxResolutionExpiresAt(),
                existing.revision(),
                existing.lastUpdatedAt()));
    }

    void applyScoresUpdated(ScoresUpdatedPayload payload) {
        for (var update : payload.updates()) {
            gamePlayers
                    .findByGameIdAndPlayerId(payload.gameId(), update.playerId())
                    .ifPresent(existing -> gamePlayers.save(
                            payload.gameId(),
                            new GamePlayer(
                                    existing.playerId(),
                                    update.newTotal(),
                                    existing.isConnected(),
                                    existing.faction())));
        }
    }

    void applyOutcomeApplied(OutcomeAppliedPayload payload) {
        lockGame(payload.gameId());
        gameActiveEvents.markResolved(payload.gameId(), payload.eventId());
        gameActiveEvents.deleteByGameIdAndEventId(payload.gameId(), payload.eventId());
    }

    void applyProbabilityStateRevealed(ProbabilityStateRevealedPayload payload) {
        if (isStaleEra(payload.gameId(), payload.eraNumber(), "ProbabilityStateRevealed")) {
            return;
        }
        revealedProbabilityIntel.upsertLatest(new RevealedProbabilityIntel(
                payload.gameId(),
                payload.playerId(),
                payload.eraNumber(),
                payload.eventId(),
                payload.roundNumber(),
                payload.outcomes().stream()
                        .map(outcome -> new RevealedProbabilityOutcome(
                                outcome.outcomeId(),
                                outcome.probability(),
                                outcome.isAnnihilated(),
                                outcome.isSealed()))
                        .toList()));
    }

    private boolean isStaleEra(UUID gameId, int eraNumber, String eventType) {
        var known = lockGame(gameId);
        if (isSupersededOrGameEnded(eraNumber, known)) {
            log.warn("{} for past/ended era {} in game {} — skipping", eventType, eraNumber, gameId);
            return true;
        }
        return false;
    }

    /**
     * Advances the per-game freshness marker for writes that do not move the phase themselves
     * (bands, declarations, expose facts, submissions, winners) — re-saving the locked projection
     * changes no coordinates and only bumps {@code revision}/{@code lastUpdatedAt} in the adapter.
     */
    private void touchRevision(UUID gameId) {
        gameProjections.save(lockGame(gameId));
    }

    void applyInfluenceTraced(InfluenceTracedPayload payload) {
        if (isStaleEra(payload.gameId(), payload.eraNumber(), "InfluenceTraced")) {
            return;
        }
        revealedInfluenceIntel.upsertLatest(new RevealedInfluenceIntel(
                payload.gameId(),
                payload.playerId(),
                payload.eraNumber(),
                payload.targetEventId(),
                payload.roundNumber(),
                payload.influencerPlayerIds() == null ? List.of() : payload.influencerPlayerIds()));
    }

    // Intercept is observe-only: the target's hand is never mutated here — only CardPlayed
    // removes cards. The intel identity's eventId carries the observed target player.
    void applyHandCardIntercepted(HandCardInterceptedPayload payload) {
        if (isStaleEra(payload.gameId(), payload.eraNumber(), "HandCardIntercepted")) {
            return;
        }
        var cards = payload.revealedCards() == null
                ? List.<RevealedHandCard>of()
                : payload.revealedCards().stream()
                        .map(card -> new RevealedHandCard(
                                card.cardInstanceId(),
                                card.cardType().name(),
                                card.grade().name()))
                        .toList();
        revealedHandCardIntel.upsertLatest(new RevealedHandCardIntel(
                payload.gameId(),
                payload.playerId(),
                payload.eraNumber(),
                payload.targetPlayerId(),
                payload.roundNumber(),
                cards));
    }

    // Neither band publication carries a round: the preview fires when Round 2 closes and the correction
    // replays that same Round-2 close, so both are stored with observedInRound 2 rather than inventing a
    // round from consumption order. The correction replaces the preview wholesale for the game and era.
    private static final int BAND_OBSERVED_IN_ROUND = 2;

    void applyBandedProbabilityPublished(BandedProbabilityPublishedPayload payload) {
        if (isStaleEra(payload.gameId(), payload.eraNumber(), "BandedProbabilityPublished")) {
            return;
        }
        publicBands.replaceAll(
                payload.gameId(),
                payload.eraNumber(),
                payload.eventStates().stream()
                        .map(event -> new PublicBand(
                                payload.gameId(),
                                payload.eraNumber(),
                                event.eventId(),
                                BAND_OBSERVED_IN_ROUND,
                                event.outcomes().stream()
                                        .map(outcome -> new PublicBand.OutcomeBand(
                                                outcome.outcomeId(),
                                                outcome.band().name()))
                                        .toList()))
                        .toList());
        touchRevision(payload.gameId());
    }

    void applyAdjustedBandsPublished(AdjustedBandsPublishedPayload payload) {
        if (isStaleEra(payload.gameId(), payload.eraNumber(), "AdjustedBandsPublished")) {
            return;
        }
        publicBands.replaceAll(
                payload.gameId(),
                payload.eraNumber(),
                payload.eventStates().stream()
                        .map(event -> new PublicBand(
                                payload.gameId(),
                                payload.eraNumber(),
                                event.eventId(),
                                BAND_OBSERVED_IN_ROUND,
                                event.outcomes().stream()
                                        .map(outcome -> new PublicBand.OutcomeBand(
                                                outcome.outcomeId(),
                                                outcome.band().name()))
                                        .toList()))
                        .toList());
        touchRevision(payload.gameId());
    }

    void applyActivistDeclarationRecorded(ActivistDeclarationRecordedPayload payload) {
        if (isStaleEra(payload.gameId(), payload.eraNumber(), "ActivistDeclarationRecorded")) {
            return;
        }
        publicDeclarations.upsert(new PublicDeclaration(
                payload.gameId(),
                payload.eraNumber(),
                payload.playerId(),
                payload.mode().name(),
                payload.targetEventId(),
                payload.targetOutcomeId()));
        playerSubmissions.upsert(new PlayerSubmission(
                payload.gameId(),
                payload.playerId(),
                payload.eraNumber(),
                null,
                PlayerSubmission.SubmissionKind.DECLARATION,
                null));
        touchRevision(payload.gameId());
    }

    void applyExposeSignatureRevealed(ExposeSignatureRevealedPayload payload) {
        if (isStaleEra(payload.gameId(), payload.eraNumber(), "ExposeSignatureRevealed")) {
            return;
        }
        var signature = payload.signature();
        exposeFacts.upsert(new ExposeFact(
                payload.gameId(),
                payload.eraNumber(),
                payload.activistPlayerId(),
                payload.targetPlayerId(),
                payload.roundNumber(),
                signature.type().name(),
                signature.targetEventId(),
                signature.sourceOutcomeId(),
                signature.targetOutcomeId(),
                false));
        touchRevision(payload.gameId());
    }

    void applyExposeBehaviorChanged(ExposeBehaviorChangedPayload payload) {
        if (isStaleEra(payload.gameId(), payload.eraNumber(), "ExposeBehaviorChanged")) {
            return;
        }
        exposeFacts.upsert(new ExposeFact(
                payload.gameId(),
                payload.eraNumber(),
                payload.activistPlayerId(),
                payload.targetPlayerId(),
                payload.roundNumber(),
                null,
                null,
                null,
                null,
                true));
        touchRevision(payload.gameId());
    }

    void applyWinConditionMet(WinConditionMetPayload payload) {
        terminalResults.addWinners(
                payload.gameId(),
                List.of(new TerminalResult.TerminalWinner(
                        payload.winnerId(), payload.faction().name())));
        touchRevision(payload.gameId());
    }

    void applyTimelineCollapsed(TimelineCollapsedPayload payload) {
        if (isStaleEra(payload.gameId(), payload.eraNumber(), "TimelineCollapsed")) {
            return;
        }
        terminalResults.addWinners(
                payload.gameId(),
                payload.winners().stream()
                        .map(winner -> new TerminalResult.TerminalWinner(
                                winner.playerId(), winner.faction().name()))
                        .toList());
        touchRevision(payload.gameId());
    }

    void applyTimelineStabilized(TimelineStabilizedPayload payload) {
        terminalResults.addWinners(
                payload.gameId(),
                payload.winners().stream()
                        .map(winner -> new TerminalResult.TerminalWinner(
                                winner.playerId(), winner.faction().name()))
                        .toList());
        touchRevision(payload.gameId());
    }

    // New chainId always replaces the tracked chain (a new chain has started); a message for the tracked
    // chainId
    // is rejected once that chain is terminal, and ChainLinkAdded is additionally rejected when its
    // chainLength does not exceed the tracked length — guards a message with no eraNumber to key staleness on.
    void applyChainLinkAdded(ChainLinkAddedPayload payload) {
        if (lockGame(payload.gameId()).phase() == Phase.GAME_ENDED) {
            log.warn("ChainLinkAdded for ended game {} — skipping", payload.gameId());
            return;
        }
        var existing = gameChains.findByGameId(payload.gameId());
        if (isStaleChainMessage(existing, payload.chainId())) {
            log.warn("ChainLinkAdded for resolved chain {} in game {} — skipping", payload.chainId(), payload.gameId());
            return;
        }
        if (isSameChain(existing, payload.chainId())
                && payload.chainLength() <= existing.get().length()) {
            log.warn(
                    "ChainLinkAdded stale length {} for chain {} in game {} — skipping",
                    payload.chainLength(),
                    payload.chainId(),
                    payload.gameId());
            return;
        }
        gameChains.save(
                payload.gameId(),
                new GameChain(payload.gameId(), payload.chainId(), ChainStatus.ACTIVE, payload.chainLength()));
    }

    void applyChainCompleted(ChainCompletedPayload payload) {
        if (lockGame(payload.gameId()).phase() == Phase.GAME_ENDED) {
            log.warn("ChainCompleted for ended game {} — skipping", payload.gameId());
            return;
        }
        var existing = gameChains.findByGameId(payload.gameId());
        if (isStaleChainMessage(existing, payload.chainId())) {
            log.warn("ChainCompleted for resolved chain {} in game {} — skipping", payload.chainId(), payload.gameId());
            return;
        }
        gameChains.save(
                payload.gameId(),
                new GameChain(
                        payload.gameId(),
                        payload.chainId(),
                        ChainStatus.COMPLETED,
                        payload.links().size()));
    }

    void applyChainBroken(ChainBrokenPayload payload) {
        if (lockGame(payload.gameId()).phase() == Phase.GAME_ENDED) {
            log.warn("ChainBroken for ended game {} — skipping", payload.gameId());
            return;
        }
        var existing = gameChains.findByGameId(payload.gameId());
        if (isStaleChainMessage(existing, payload.chainId())) {
            log.warn("ChainBroken for resolved chain {} in game {} — skipping", payload.chainId(), payload.gameId());
            return;
        }
        gameChains.save(
                payload.gameId(),
                new GameChain(payload.gameId(), payload.chainId(), ChainStatus.BROKEN, payload.chainLengthAtBreak()));
    }

    private boolean isSameChain(Optional<GameChain> existing, UUID chainId) {
        return existing.isPresent() && existing.get().chainId().equals(chainId);
    }

    private boolean isStaleChainMessage(Optional<GameChain> existing, UUID chainId) {
        return isSameChain(existing, chainId) && existing.get().status() != ChainStatus.ACTIVE;
    }

    void applyParadoxResolutionPhaseStarted(ParadoxResolutionPhaseStartedPayload payload, Instant occurredAt) {
        var existing = lockGame(payload.gameId());
        if (isSupersededOrGameEnded(payload.eraNumber(), existing)) {
            log.warn(
                    "ParadoxResolutionPhaseStarted for past/ended era {} in game {} — skipping",
                    payload.eraNumber(),
                    payload.gameId());
            return;
        }
        var expiresAt = occurredAt == null ? null : occurredAt.plusSeconds(payload.timerSeconds());
        gameProjections.save(new GameProjection(
                existing.gameId(),
                payload.eraNumber(),
                Phase.PARADOX_RESOLUTION,
                payload.paradoxIds(),
                existing.lastRoundSummary(),
                existing.currentRoundNumber(),
                null,
                expiresAt,
                existing.revision(),
                existing.lastUpdatedAt()));
    }

    void applyParadoxResolved(ParadoxResolvedPayload payload) {
        closeParadoxId(payload.gameId(), payload.eraNumber(), payload.paradoxId());
    }

    void applyParadoxCascaded(ParadoxCascadedPayload payload) {
        closeParadoxId(payload.gameId(), payload.eraNumber(), payload.paradoxId());
    }

    private void closeParadoxId(UUID gameId, int eraNumber, UUID paradoxId) {
        var existing = lockGame(gameId);
        if (isSupersededOrGameEnded(eraNumber, existing)) {
            log.warn("Paradox resolution for past/ended era {} in game {} — skipping", eraNumber, gameId);
            return;
        }
        if (!existing.pendingParadoxIds().contains(paradoxId)) {
            log.warn("Paradox {} not pending for game {} — skipping", paradoxId, gameId);
            return;
        }
        var stillPending = existing.pendingParadoxIds().stream()
                .filter(pending -> !pending.equals(paradoxId))
                .toList();
        var phase = stillPending.isEmpty() ? Phase.RESOLUTION : Phase.PARADOX_RESOLUTION;
        // The closing round leaves no open coordinate behind: expiry and round clear with the phase.
        var roundNumber = stillPending.isEmpty() ? null : existing.currentRoundNumber();
        var paradoxExpiresAt = stillPending.isEmpty() ? null : existing.paradoxResolutionExpiresAt();
        gameProjections.save(new GameProjection(
                existing.gameId(),
                eraNumber,
                phase,
                stillPending,
                existing.lastRoundSummary(),
                roundNumber,
                existing.actionRoundExpiresAt(),
                paradoxExpiresAt,
                existing.revision(),
                existing.lastUpdatedAt()));
    }

    private GameProjection lockGame(UUID gameId) {
        return gameProjections
                .findByGameIdForUpdate(gameId)
                .orElseThrow(() -> new IllegalStateException("Game projection anchor was not created for " + gameId));
    }

    private boolean isSupersededOrGameEnded(int payloadEraNumber, GameProjection known) {
        return known.phase() == Phase.GAME_ENDED
                || payloadEraNumber < known.eraNumber()
                || (payloadEraNumber == known.eraNumber() && known.phase().isEraOver());
    }

    private boolean isStaleRoundSummary(RoundSummaryPublishedPayload payload, LastRoundSummary known) {
        return known != null
                && (payload.eraNumber() < known.eraNumber()
                        || (payload.eraNumber() == known.eraNumber() && payload.roundNumber() <= known.roundNumber()));
    }

    // isSupersededOrGameEnded only catches an era that has fully ended. A same-era message that targets a
    // phase the projection already passed (e.g. a redelivered EraStarted arriving during ACTION_ROUND_3, or
    // ResolutionStarted arriving during PARADOX_RESOLUTION) would otherwise silently roll phase backward.
    // PARADOX_RESOLUTION and RESOLUTION share a rank because closeParadoxId's own PARADOX_RESOLUTION ->
    // RESOLUTION transition is the legitimate forward move out of paradox handling, not a regression.
    private boolean regressesPhaseWithinEra(GameProjection known, int payloadEraNumber, Phase target) {
        return payloadEraNumber == known.eraNumber() && phaseRank(known.phase()) >= phaseRank(target);
    }

    private int phaseRank(Phase phase) {
        return switch (phase) {
            case LOBBY -> 0;
            case ERA_START -> 1;
            case ACTION_ROUND_1 -> 2;
            case ACTION_ROUND_2 -> 3;
            case ACTION_ROUND_3 -> 4;
            case RESOLUTION, PARADOX_RESOLUTION -> 5;
            case ERA_END -> 6;
            case GAME_ENDED -> 7;
        };
    }
}
