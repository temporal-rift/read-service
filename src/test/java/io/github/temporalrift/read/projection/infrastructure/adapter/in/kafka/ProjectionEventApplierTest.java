package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActionRoundStartedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardPlayedPayload;
import io.github.temporalrift.asyncapi.scoringevents.GeneratedChannelContract.ScoresUpdatedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EraEndedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EraStartedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventsDrawnFutureEvent;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventsDrawnOutcome;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventsDrawnPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.Faction;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.FactionAssignedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.FactionRevealedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.FactionRevealedPlayerFactionResult;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GameEndedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GameEndedPlayerScoreResult;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GameStartedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandDealtCardInstance;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandDealtPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandSelectedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandSelectionOrigin;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PlayerDisconnectedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.ResolutionStartedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.OutcomeAppliedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxCascadedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxResolutionPhaseStartedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxResolvedPayload;
import io.github.temporalrift.read.projection.domain.model.CarryOverState;
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

@ExtendWith(MockitoExtension.class)
class ProjectionEventApplierTest {

    @Mock
    GameProjectionRepository gameProjections;

    @Mock
    GamePlayerRepository gamePlayers;

    @Mock
    GameActiveEventRepository gameActiveEvents;

    @Mock
    PlayerGameStateRepository playerGameStates;

    private ProjectionEventApplier applier;

    private final UUID gameId = UUID.randomUUID();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        applier = new ProjectionEventApplier(gameProjections, gamePlayers, gameActiveEvents, playerGameStates);
    }

    @Test
    void applyGameStarted_createsGameProjectionAndOneRowPerPlayer() {
        var player1 = UUID.randomUUID();
        var player2 = UUID.randomUUID();
        given(gamePlayers.findByGameIdAndPlayerId(eq(gameId), any())).willReturn(Optional.empty());
        given(playerGameStates.findByGameIdAndPlayerId(eq(gameId), any())).willReturn(Optional.empty());

        applier.applyGameStarted(new GameStartedPayload(gameId, UUID.randomUUID(), List.of(player1, player2), 3, 30));

        then(gameProjections).should().save(new GameProjection(gameId, 0, Phase.LOBBY));
        then(gamePlayers).should().save(gameId, new GamePlayer(player1, 0, true, null));
        then(gamePlayers).should().save(gameId, new GamePlayer(player2, 0, true, null));
        then(playerGameStates).should().save(new PlayerGameState(gameId, player1, null, List.of()));
        then(playerGameStates).should().save(new PlayerGameState(gameId, player2, null, List.of()));
    }

    @Test
    void applyGameStarted_afterFactionAssignedAlreadyArrived_preservesFactionRatherThanResetting() {
        var playerId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of())));
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        applier.applyGameStarted(new GameStartedPayload(gameId, UUID.randomUUID(), List.of(playerId), 3, 30));

        then(playerGameStates).should().save(new PlayerGameState(gameId, playerId, "ERASERS", List.of()));
    }

    @Test
    void applyGameStarted_afterHandDealtAlreadyArrived_preservesHandRatherThanResetting() {
        var playerId = UUID.randomUUID();
        var card = new HandCard(UUID.randomUUID(), "PUSH");
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, null, List.of(card))));
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        applier.applyGameStarted(new GameStartedPayload(gameId, UUID.randomUUID(), List.of(playerId), 3, 30));

        then(playerGameStates).should().save(new PlayerGameState(gameId, playerId, null, List.of(card)));
    }

    @Test
    void applyGameStarted_afterPlayerDisconnectedAlreadyArrived_preservesConnectionStateRatherThanResetting() {
        var playerId = UUID.randomUUID();
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new GamePlayer(playerId, 0, false, null)));
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        applier.applyGameStarted(new GameStartedPayload(gameId, UUID.randomUUID(), List.of(playerId), 3, 30));

        then(gamePlayers).should().save(gameId, new GamePlayer(playerId, 0, false, null));
    }

    @Test
    void applyFactionAssigned_updatesOnlyThatPlayersOwnFaction() {
        var playerId = UUID.randomUUID();
        var existing = new PlayerGameState(gameId, playerId, null, List.of());
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.of(existing));

        applier.applyFactionAssigned(new FactionAssignedPayload(gameId, playerId, Faction.ERASERS));

        then(playerGameStates).should().save(new PlayerGameState(gameId, playerId, "ERASERS", List.of()));
    }

    @Test
    void applyFactionAssigned_arrivesBeforeGameStarted_createsRowRatherThanDropping() {
        // IncompleteEventPublicationResubmitter can resubmit a failed send out of original order, so
        // FactionAssigned reaching read-service before GameStarted's row exists is a real possibility,
        // not just a hypothetical — it must not be silently dropped.
        var playerId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        applier.applyFactionAssigned(new FactionAssignedPayload(gameId, playerId, Faction.ERASERS));

        then(playerGameStates).should().save(new PlayerGameState(gameId, playerId, "ERASERS", List.of()));
    }

    @Test
    void applyEraStarted_setsEraNumberAndPhase() {
        applier.applyEraStarted(new EraStartedPayload(gameId, 2, List.of(), List.of(UUID.randomUUID())));

        then(gameProjections).should().save(new GameProjection(gameId, 2, Phase.ERA_START));
    }

    @Test
    void applyEventsDrawn_savesOneActiveEventPerDrawnEvent() {
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        var payload = new EventsDrawnPayload(
                gameId,
                1,
                List.of(new EventsDrawnFutureEvent(
                        eventId,
                        "Title",
                        List.of(new EventsDrawnOutcome(outcomeId, "desc", 50)),
                        io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CarryOverState.FRESH)));

        applier.applyEventsDrawn(payload);

        var captor = ArgumentCaptor.forClass(GameActiveEvent.class);
        then(gameActiveEvents).should().save(eq(gameId), captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo(eventId);
        assertThat(captor.getValue().carryOverState()).isEqualTo(CarryOverState.FRESH);
        assertThat(captor.getValue().outcomes())
                .containsExactly(
                        new io.github.temporalrift.read.projection.domain.model.EventOutcome(outcomeId, "desc"));
    }

    @Test
    void applyEventsDrawn_preservesStalledCarryOverState() {
        var eventId = UUID.randomUUID();
        var payload = new EventsDrawnPayload(
                gameId,
                2,
                List.of(new EventsDrawnFutureEvent(
                        eventId,
                        "Stalled Event",
                        List.of(),
                        io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CarryOverState
                                .STALLED)));

        applier.applyEventsDrawn(payload);

        var captor = ArgumentCaptor.forClass(GameActiveEvent.class);
        then(gameActiveEvents).should().save(eq(gameId), captor.capture());
        assertThat(captor.getValue())
                .extracting(GameActiveEvent::eventId, GameActiveEvent::carryOverState)
                .containsExactly(eventId, CarryOverState.STALLED);
    }

    @Test
    void applyHandDealt_replacesRatherThanAppendsToExistingHand() {
        // game-service deals a full fresh hand each era and replaces PlayerState's hand wholesale
        // (ActionStateProjectionEventListener.onHandDealt) — this side must match, or the projected
        // hand accumulates every era's cards without bound instead of reflecting only the current one.
        var playerId = UUID.randomUUID();
        var eraOneCard = new HandCard(UUID.randomUUID(), "SCAN");
        var eraTwoCardId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(eraOneCard))));

        applier.applyHandDealt(new HandDealtPayload(
                gameId,
                2,
                playerId,
                Instant.EPOCH,
                List.of(new HandDealtCardInstance(eraTwoCardId, CardType.PUSH, CardGrade.II, 0))));

        var captor = ArgumentCaptor.forClass(PlayerGameState.class);
        then(playerGameStates).should().save(captor.capture());
        assertThat(captor.getValue().myHand()).containsExactly(new HandCard(eraTwoCardId, "PUSH"));
    }

    @Test
    void applyHandDealt_arrivesBeforeGameStarted_createsRowRatherThanDropping() {
        var playerId = UUID.randomUUID();
        var cardId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        applier.applyHandDealt(new HandDealtPayload(
                gameId,
                1,
                playerId,
                Instant.EPOCH,
                List.of(new HandDealtCardInstance(cardId, CardType.PUSH, CardGrade.II, 0))));

        then(playerGameStates)
                .should()
                .save(new PlayerGameState(gameId, playerId, null, List.of(new HandCard(cardId, "PUSH"))));
    }

    @Test
    void applyHandSelected_replacesPendingOfferWithKeptHand() {
        // HandDealt now carries the pending 7-card offer (game-service#121/#127); HandSelected carries the
        // terminal 5-card kept hand once selection resolves, whether by player choice or the hand-selection
        // timer's random default — the projected hand must reflect the kept cards, not the offer.
        var playerId = UUID.randomUUID();
        var offeredCard = new HandCard(UUID.randomUUID(), "SCAN");
        var keptCardId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(offeredCard))));

        applier.applyHandSelected(new HandSelectedPayload(
                gameId,
                1,
                playerId,
                HandSelectionOrigin.PLAYER,
                List.of(new HandDealtCardInstance(keptCardId, CardType.PUSH, CardGrade.II, 0))));

        var captor = ArgumentCaptor.forClass(PlayerGameState.class);
        then(playerGameStates).should().save(captor.capture());
        assertThat(captor.getValue().myHand()).containsExactly(new HandCard(keptCardId, "PUSH"));
    }

    @Test
    void applyHandDealt_arrivesAfterHandSelectedForSameEra_isDropped() {
        // IncompleteEventPublicationResubmitter can resubmit a failed send out of original order, so the
        // seven-card offer for an era can still arrive after that era's HandSelected was already applied.
        // Applying it anyway would silently revert the kept five-card hand back to the stale offer.
        var playerId = UUID.randomUUID();
        var keptCard = new HandCard(UUID.randomUUID(), "PUSH");
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(keptCard), 1)));

        applier.applyHandDealt(new HandDealtPayload(
                gameId,
                1,
                playerId,
                Instant.EPOCH,
                List.of(new HandDealtCardInstance(UUID.randomUUID(), CardType.SCAN, CardGrade.I, 0))));

        then(playerGameStates).should(never()).save(any());
    }

    @Test
    void applyHandSelected_arrivesAfterLaterEraAlreadySelected_isDropped() {
        // Symmetric to the HandDealt guard above: a delayed earlier-era HandSelected must not regress
        // handSelectedEraNumber, or it would both corrupt the projected hand back to a stale era and reopen
        // the door for a later-era HandDealt to slip past that other guard.
        var playerId = UUID.randomUUID();
        var eraTwoCard = new HandCard(UUID.randomUUID(), "PUSH");
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(eraTwoCard), 2)));

        applier.applyHandSelected(new HandSelectedPayload(
                gameId,
                1,
                playerId,
                HandSelectionOrigin.PLAYER,
                List.of(new HandDealtCardInstance(UUID.randomUUID(), CardType.SCAN, CardGrade.I, 0))));

        then(playerGameStates).should(never()).save(any());
    }

    @Test
    void applyHandSelected_timeoutRandomOrigin_arrivesBeforeGameStarted_createsRowRatherThanDropping() {
        var playerId = UUID.randomUUID();
        var cardId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        applier.applyHandSelected(new HandSelectedPayload(
                gameId,
                1,
                playerId,
                HandSelectionOrigin.TIMEOUT_RANDOM,
                List.of(new HandDealtCardInstance(cardId, CardType.PUSH, CardGrade.II, 0))));

        then(playerGameStates)
                .should()
                .save(new PlayerGameState(gameId, playerId, null, List.of(new HandCard(cardId, "PUSH")), 1));
    }

    @Test
    void applyPlayerDisconnected_setsIsConnectedFalse() {
        var playerId = UUID.randomUUID();
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new GamePlayer(playerId, 5, true, null)));

        applier.applyPlayerDisconnected(new PlayerDisconnectedPayload(gameId, playerId));

        then(gamePlayers).should().save(gameId, new GamePlayer(playerId, 5, false, null));
    }

    @Test
    void applyPlayerDisconnected_arrivesBeforeGameStarted_createsRowRatherThanDropping() {
        var playerId = UUID.randomUUID();
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        applier.applyPlayerDisconnected(new PlayerDisconnectedPayload(gameId, playerId));

        then(gamePlayers).should().save(gameId, new GamePlayer(playerId, 0, false, null));
    }

    @Test
    void applyEraEnded_setsPhaseAndClearsActiveEvents() {
        applier.applyEraEnded(new EraEndedPayload(gameId, 1, 0, 2));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.ERA_END));
        then(gameActiveEvents).should().deleteByGameId(gameId);
    }

    @Test
    void applyGameEnded_preservesEraNumberAndUpdatesScores() {
        var playerId = UUID.randomUUID();
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 3, Phase.RESOLUTION)));
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new GamePlayer(playerId, 10, true, "ERASERS")));

        applier.applyGameEnded(new GameEndedPayload(
                gameId, "SCORE_THRESHOLD", List.of(new GameEndedPlayerScoreResult(playerId, Faction.ERASERS, 20))));

        then(gameProjections).should().save(new GameProjection(gameId, 3, Phase.GAME_ENDED));
        then(gamePlayers).should().save(gameId, new GamePlayer(playerId, 20, true, "ERASERS"));
    }

    @Test
    void applyFactionRevealed_updatesFactionForEachReveal() {
        var playerId = UUID.randomUUID();
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new GamePlayer(playerId, 5, true, null)));

        applier.applyFactionRevealed(new FactionRevealedPayload(
                gameId, List.of(new FactionRevealedPlayerFactionResult(playerId, Faction.WEAVERS))));

        then(gamePlayers).should().save(gameId, new GamePlayer(playerId, 5, true, "WEAVERS"));
    }

    @Test
    void applyResolutionStarted_setsPhase() {
        applier.applyResolutionStarted(new ResolutionStartedPayload(gameId, 1));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.RESOLUTION));
    }

    @Test
    void applyActionRoundStarted_mapsRoundNumberToPhase() {
        applier.applyActionRoundStarted(new ActionRoundStartedPayload(gameId, 1, 2, 45, List.of()));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.ACTION_ROUND_2));
    }

    @Test
    void applyActionRoundStarted_unsupportedRoundNumber_throws() {
        var payload = new ActionRoundStartedPayload(gameId, 1, 4, 45, List.of());

        assertThatThrownBy(() -> applier.applyActionRoundStarted(payload)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyCardPlayed_removesPlayedCardFromHand() {
        var playerId = UUID.randomUUID();
        var playedCard = new HandCard(UUID.randomUUID(), "PUSH");
        var otherCard = new HandCard(UUID.randomUUID(), "SCAN");
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(
                        Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(playedCard, otherCard))));

        applier.applyCardPlayed(new CardPlayedPayload(
                gameId,
                1,
                1,
                playerId,
                playedCard.cardInstanceId(),
                io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardType.PUSH,
                io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardGrade.II,
                UUID.randomUUID(),
                null,
                UUID.randomUUID()));

        var captor = ArgumentCaptor.forClass(PlayerGameState.class);
        then(playerGameStates).should().save(captor.capture());
        assertThat(captor.getValue().myHand()).containsExactly(otherCard);
    }

    @Test
    void applyScoresUpdated_setsNewTotalPerUpdate() {
        var playerId = UUID.randomUUID();
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new GamePlayer(playerId, 4, true, "PROPHETS")));

        applier.applyScoresUpdated(new ScoresUpdatedPayload(
                gameId,
                1,
                List.of(new io.github.temporalrift.asyncapi.scoringevents.GeneratedChannelContract.ScoreUpdate(
                        playerId,
                        io.github.temporalrift.asyncapi.scoringevents.GeneratedChannelContract.Faction.PROPHETS,
                        4,
                        "REASON",
                        8))));

        then(gamePlayers).should().save(gameId, new GamePlayer(playerId, 8, true, "PROPHETS"));
    }

    @Test
    void applyOutcomeApplied_removesEventFromActiveEvents() {
        var eventId = UUID.randomUUID();

        applier.applyOutcomeApplied(new OutcomeAppliedPayload(gameId, 1, eventId, UUID.randomUUID(), List.of()));

        then(gameActiveEvents).should().deleteByGameIdAndEventId(gameId, eventId);
    }

    @Test
    void applyParadoxResolutionPhaseStarted_opensPhaseWithPendingParadoxIds() {
        var paradox1 = UUID.randomUUID();
        var paradox2 = UUID.randomUUID();
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.RESOLUTION)));

        applier.applyParadoxResolutionPhaseStarted(
                new ParadoxResolutionPhaseStartedPayload(gameId, 1, List.of(paradox1, paradox2), 60));

        then(gameProjections)
                .should()
                .save(new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradox1, paradox2)));
    }

    @Test
    void applyParadoxResolved_oneOfTwoPending_keepsPhaseOpen() {
        var paradox1 = UUID.randomUUID();
        var paradox2 = UUID.randomUUID();
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(
                        new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradox1, paradox2))));

        applier.applyParadoxResolved(new ParadoxResolvedPayload(gameId, 1, paradox1, UUID.randomUUID()));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradox2)));
    }

    @Test
    void applyParadoxCascaded_lastPending_closesPhaseBackToResolution() {
        var paradoxId = UUID.randomUUID();
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradoxId))));

        applier.applyParadoxCascaded(
                new ParadoxCascadedPayload(gameId, 1, paradoxId, UUID.randomUUID(), List.of(), null));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.RESOLUTION, List.of()));
    }

    @Test
    void applyParadoxResolved_mixedWithCascade_closesOnceBothTerminal() {
        var paradox1 = UUID.randomUUID();
        var paradox2 = UUID.randomUUID();
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(
                        new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradox1, paradox2))));

        applier.applyParadoxResolved(new ParadoxResolvedPayload(gameId, 1, paradox1, UUID.randomUUID()));

        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradox2))));

        applier.applyParadoxCascaded(
                new ParadoxCascadedPayload(gameId, 1, paradox2, UUID.randomUUID(), List.of(), null));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.RESOLUTION, List.of()));
    }

    @Test
    void applyParadoxResolved_notPending_isNoOp() {
        var paradoxId = UUID.randomUUID();
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.RESOLUTION, List.of())));

        applier.applyParadoxResolved(new ParadoxResolvedPayload(gameId, 1, paradoxId, UUID.randomUUID()));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyParadoxResolutionPhaseStarted_unknownGame_skipsWithoutSaving() {
        given(gameProjections.findByGameId(gameId)).willReturn(Optional.empty());

        applier.applyParadoxResolutionPhaseStarted(
                new ParadoxResolutionPhaseStartedPayload(gameId, 1, List.of(UUID.randomUUID()), 60));

        then(gameProjections).should(never()).save(any());
    }
}
