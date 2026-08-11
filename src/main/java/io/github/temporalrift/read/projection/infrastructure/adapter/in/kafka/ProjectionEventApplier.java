package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.temporalrift.read.projection.domain.model.EventOutcome;
import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.GameProjection;
import io.github.temporalrift.read.projection.domain.model.HandCard;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.PlayerGameState;
import io.github.temporalrift.read.projection.domain.port.out.GameActiveEventRepository;
import io.github.temporalrift.read.projection.domain.port.out.GamePlayerRepository;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;
import io.github.temporalrift.read.projection.domain.port.out.PlayerGameStateRepository;

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

    ProjectionEventApplier(
            GameProjectionRepository gameProjections,
            GamePlayerRepository gamePlayers,
            GameActiveEventRepository gameActiveEvents,
            PlayerGameStateRepository playerGameStates) {
        this.gameProjections = gameProjections;
        this.gamePlayers = gamePlayers;
        this.gameActiveEvents = gameActiveEvents;
        this.playerGameStates = playerGameStates;
    }

    void applyGameStarted(GameStartedPayload payload) {
        gameProjections.save(new GameProjection(payload.gameId(), 0, Phase.LOBBY));
        for (var playerId : payload.playerIds()) {
            gamePlayers.save(payload.gameId(), new GamePlayer(playerId, 0, true, null));
            playerGameStates.save(new PlayerGameState(payload.gameId(), playerId, null, List.of()));
        }
    }

    // Per-player events are not guaranteed to arrive after GameStarted creates the row they target —
    // IncompleteEventPublicationResubmitter can resubmit a failed send out of original order, so
    // delivery order is not a safe assumption. Find-or-create rather than skip-on-missing, matching
    // game-service's own ActionStateProjectionEventListener.onHandDealt precedent for the same race.
    void applyFactionAssigned(FactionAssignedPayload payload) {
        var existing = findOrCreatePlayerGameState(payload.gameId(), payload.playerId());
        playerGameStates.save(
                new PlayerGameState(existing.gameId(), existing.playerId(), payload.faction(), existing.myHand()));
    }

    void applyEraStarted(EraStartedPayload payload) {
        gameProjections.save(new GameProjection(payload.gameId(), payload.eraNumber(), Phase.ERA_START));
    }

    void applyEventsDrawn(EventsDrawnPayload payload) {
        for (var event : payload.events()) {
            var outcomes = event.outcomes().stream()
                    .map(o -> new EventOutcome(o.outcomeId(), o.description()))
                    .toList();
            gameActiveEvents.save(
                    payload.gameId(),
                    new GameActiveEvent(event.eventId(), event.title(), event.carryOverState(), outcomes));
        }
    }

    // HandDealt replaces the hand rather than appending to it: game-service deals a full fresh hand
    // each era (EraSagaImpl) and game-service's own PlayerState is reconstituted the same way
    // (ActionStateProjectionEventListener.onHandDealt), so this side must match or the projected hand
    // grows without bound across eras.
    void applyHandDealt(HandDealtPayload payload) {
        var existing = findOrCreatePlayerGameState(payload.gameId(), payload.playerId());
        var hand = payload.cards().stream()
                .map(card -> new HandCard(card.cardInstanceId(), card.cardType()))
                .toList();
        playerGameStates.save(new PlayerGameState(existing.gameId(), existing.playerId(), existing.myFaction(), hand));
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
        var existing = gamePlayers
                .findByGameIdAndPlayerId(gameId, playerId)
                .orElseGet(() -> new GamePlayer(playerId, 0, connected, null));
        gamePlayers.save(gameId, new GamePlayer(existing.playerId(), existing.score(), connected, existing.faction()));
    }

    void applyEraEnded(EraEndedPayload payload) {
        gameProjections.save(new GameProjection(payload.gameId(), payload.eraNumber(), Phase.ERA_END));
        // Defensive clear — design.md Decision 6. Every drawn event currently gets an OutcomeApplied (no
        // cascade/paradox handling exists yet), so this is normally a no-op.
        gameActiveEvents.deleteByGameId(payload.gameId());
    }

    void applyGameEnded(GameEndedPayload payload) {
        var eraNumber = gameProjections
                .findByGameId(payload.gameId())
                .map(GameProjection::eraNumber)
                .orElse(0);
        gameProjections.save(new GameProjection(payload.gameId(), eraNumber, Phase.GAME_ENDED));
        for (var finalScore : payload.finalScores()) {
            gamePlayers
                    .findByGameIdAndPlayerId(payload.gameId(), finalScore.playerId())
                    .ifPresent(existing -> gamePlayers.save(
                            payload.gameId(),
                            new GamePlayer(
                                    existing.playerId(),
                                    finalScore.score(),
                                    existing.isConnected(),
                                    existing.faction())));
        }
    }

    void applyFactionRevealed(FactionRevealedPayload payload) {
        for (var reveal : payload.reveals()) {
            gamePlayers
                    .findByGameIdAndPlayerId(payload.gameId(), reveal.playerId())
                    .ifPresent(existing -> gamePlayers.save(
                            payload.gameId(),
                            new GamePlayer(
                                    existing.playerId(), existing.score(), existing.isConnected(), reveal.faction())));
        }
    }

    void applyResolutionStarted(ResolutionStartedPayload payload) {
        gameProjections.save(new GameProjection(payload.gameId(), payload.eraNumber(), Phase.RESOLUTION));
    }

    void applyActionRoundStarted(ActionRoundStartedPayload payload) {
        var phase =
                switch (payload.roundNumber()) {
                    case 1 -> Phase.ACTION_ROUND_1;
                    case 2 -> Phase.ACTION_ROUND_2;
                    case 3 -> Phase.ACTION_ROUND_3;
                    default -> throw new IllegalArgumentException("Unsupported roundNumber " + payload.roundNumber());
                };
        gameProjections.save(new GameProjection(payload.gameId(), payload.eraNumber(), phase));
    }

    void applyCardPlayed(CardPlayedPayload payload) {
        playerGameStates
                .findByGameIdAndPlayerId(payload.gameId(), payload.playerId())
                .ifPresentOrElse(
                        existing -> {
                            var hand = existing.myHand().stream()
                                    .filter(card -> !card.cardInstanceId().equals(payload.cardInstanceId()))
                                    .toList();
                            playerGameStates.save(new PlayerGameState(
                                    existing.gameId(), existing.playerId(), existing.myFaction(), hand));
                        },
                        () -> log.warn(
                                "CardPlayed for unknown player {} in game {} — skipping",
                                payload.playerId(),
                                payload.gameId()));
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
        gameActiveEvents.deleteByGameIdAndEventId(payload.gameId(), payload.eventId());
    }

    void applyParadoxResolutionPhaseStarted(ParadoxResolutionPhaseStartedPayload payload) {
        gameProjections
                .findByGameId(payload.gameId())
                .ifPresentOrElse(
                        existing -> gameProjections.save(new GameProjection(
                                existing.gameId(),
                                existing.eraNumber(),
                                Phase.PARADOX_RESOLUTION,
                                payload.paradoxIds())),
                        () -> log.warn(
                                "ParadoxResolutionPhaseStarted for unknown game {} — skipping", payload.gameId()));
    }

    void applyParadoxResolved(ParadoxResolvedPayload payload) {
        closeParadoxId(payload.gameId(), payload.paradoxId());
    }

    void applyParadoxCascaded(ParadoxCascadedPayload payload) {
        closeParadoxId(payload.gameId(), payload.paradoxId());
    }

    private void closeParadoxId(UUID gameId, UUID paradoxId) {
        gameProjections
                .findByGameId(gameId)
                .ifPresentOrElse(
                        existing -> {
                            if (!existing.pendingParadoxIds().contains(paradoxId)) {
                                log.warn("Paradox {} not pending for game {} — skipping", paradoxId, gameId);
                                return;
                            }
                            var stillPending = existing.pendingParadoxIds().stream()
                                    .filter(pending -> !pending.equals(paradoxId))
                                    .toList();
                            var phase = stillPending.isEmpty() ? Phase.RESOLUTION : Phase.PARADOX_RESOLUTION;
                            gameProjections.save(
                                    new GameProjection(existing.gameId(), existing.eraNumber(), phase, stillPending));
                        },
                        () -> log.warn("Paradox resolution for unknown game {} — skipping", gameId));
    }
}
