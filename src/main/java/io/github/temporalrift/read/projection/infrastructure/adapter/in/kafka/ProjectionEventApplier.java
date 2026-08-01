package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.ArrayList;
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

    void applyFactionAssigned(FactionAssignedPayload payload) {
        playerGameStates
                .findByGameIdAndPlayerId(payload.gameId(), payload.playerId())
                .ifPresentOrElse(
                        existing -> playerGameStates.save(new PlayerGameState(
                                existing.gameId(), existing.playerId(), payload.faction(), existing.myHand())),
                        () -> log.warn(
                                "FactionAssigned for unknown player {} in game {} — skipping",
                                payload.playerId(),
                                payload.gameId()));
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
                    new GameActiveEvent(event.eventId(), event.title(), event.isCascaded(), outcomes));
        }
    }

    void applyHandDealt(HandDealtPayload payload) {
        playerGameStates
                .findByGameIdAndPlayerId(payload.gameId(), payload.playerId())
                .ifPresentOrElse(
                        existing -> {
                            var hand = new ArrayList<>(existing.myHand());
                            payload.cards()
                                    .forEach(card -> hand.add(new HandCard(card.cardInstanceId(), card.cardType())));
                            playerGameStates.save(new PlayerGameState(
                                    existing.gameId(), existing.playerId(), existing.myFaction(), hand));
                        },
                        () -> log.warn(
                                "HandDealt for unknown player {} in game {} — skipping",
                                payload.playerId(),
                                payload.gameId()));
    }

    void applyPlayerDisconnected(PlayerDisconnectedPayload payload) {
        setConnected(payload.gameId(), payload.playerId(), false);
    }

    void applyPlayerAbandoned(PlayerAbandonedPayload payload) {
        setConnected(payload.gameId(), payload.playerId(), false);
    }

    private void setConnected(UUID gameId, UUID playerId, boolean connected) {
        gamePlayers
                .findByGameIdAndPlayerId(gameId, playerId)
                .ifPresentOrElse(
                        existing -> gamePlayers.save(
                                gameId,
                                new GamePlayer(existing.playerId(), existing.score(), connected, existing.faction())),
                        () -> log.warn(
                                "Connection update for unknown player {} in game {} — skipping", playerId, gameId));
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
}
