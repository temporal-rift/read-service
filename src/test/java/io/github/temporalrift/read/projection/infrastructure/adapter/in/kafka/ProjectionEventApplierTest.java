package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActionRoundStartedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardPlayedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.HandCardInterceptedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.InfluenceTracedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.InterceptedHandCard;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.PlayerJammedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.RoundSummaryPublishedPayload;
import io.github.temporalrift.asyncapi.scoringevents.GeneratedChannelContract.ScoreUpdate;
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
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityStateRevealedOutcomeState;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityStateRevealedPayload;
import io.github.temporalrift.read.projection.domain.model.CarryOverState;
import io.github.temporalrift.read.projection.domain.model.EventOutcome;
import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.GameProjection;
import io.github.temporalrift.read.projection.domain.model.HandCard;
import io.github.temporalrift.read.projection.domain.model.LastRoundSummary;
import io.github.temporalrift.read.projection.domain.model.PendingHandCard;
import io.github.temporalrift.read.projection.domain.model.PendingHandSelection;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.PlayerGameState;
import io.github.temporalrift.read.projection.domain.model.RevealedHandCard;
import io.github.temporalrift.read.projection.domain.model.RevealedHandCardIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedInfluenceIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityOutcome;
import io.github.temporalrift.read.projection.domain.model.RoundActionSummary;
import io.github.temporalrift.read.projection.domain.port.out.GameActiveEventRepository;
import io.github.temporalrift.read.projection.domain.port.out.GamePlayerRepository;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;
import io.github.temporalrift.read.projection.domain.port.out.PlayerGameStateRepository;
import io.github.temporalrift.read.projection.domain.port.out.RevealedHandCardIntelRepository;
import io.github.temporalrift.read.projection.domain.port.out.RevealedInfluenceIntelRepository;
import io.github.temporalrift.read.projection.domain.port.out.RevealedProbabilityIntelRepository;

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

    @Mock
    RevealedProbabilityIntelRepository revealedProbabilityIntel;

    @Mock
    RevealedInfluenceIntelRepository revealedInfluenceIntel;

    @Mock
    RevealedHandCardIntelRepository revealedHandCardIntel;

    private ProjectionEventApplier applier;

    private final UUID gameId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient()
                .when(gameProjections.findByGameIdForUpdate(gameId))
                .thenReturn(Optional.of(new GameProjection(gameId, 0, Phase.LOBBY)));
        applier = new ProjectionEventApplier(
                gameProjections,
                gamePlayers,
                gameActiveEvents,
                playerGameStates,
                revealedProbabilityIntel,
                revealedInfluenceIntel,
                revealedHandCardIntel);
    }

    @Test
    void applyGameStarted_usesEnsuredProjectionAnchorAndCreatesOneRowPerPlayer() {
        var player1 = UUID.randomUUID();
        var player2 = UUID.randomUUID();
        given(gamePlayers.findByGameIdAndPlayerId(eq(gameId), any())).willReturn(Optional.empty());
        given(playerGameStates.findByGameIdAndPlayerId(eq(gameId), any())).willReturn(Optional.empty());

        applier.applyGameStarted(new GameStartedPayload(gameId, UUID.randomUUID(), List.of(player1, player2), 3, 30));

        then(gameProjections).should(never()).save(any());
        then(gamePlayers).should().save(gameId, new GamePlayer(player1, 0, true, null));
        then(gamePlayers).should().save(gameId, new GamePlayer(player2, 0, true, null));
        then(playerGameStates).should().save(new PlayerGameState(gameId, player1, null, List.of()));
        then(playerGameStates).should().save(new PlayerGameState(gameId, player2, null, List.of()));
    }

    @Test
    void applyGameStarted_afterActionRoundStartedAlreadyArrived_preservesNewerProjectionAndPlayerState() {
        var playerId = UUID.randomUUID();
        var existingProjection = new GameProjection(gameId, 1, Phase.ACTION_ROUND_1);
        var hand = List.of(new HandCard(UUID.randomUUID(), "PUSH"));
        var pendingHand = new PendingHandSelection(
                List.of(new PendingHandCard(UUID.randomUUID(), "SWING", "II", 0)),
                Instant.parse("2030-01-01T00:00:00Z"));
        var existingPlayerState = new PlayerGameState(gameId, playerId, "ERASERS", hand, pendingHand);
        var existingPlayer = new GamePlayer(playerId, 7, false, "ERASERS");
        given(gameProjections.findByGameIdForUpdate(gameId)).willReturn(Optional.of(existingProjection));
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.of(existingPlayerState));
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.of(existingPlayer));

        applier.applyGameStarted(new GameStartedPayload(gameId, UUID.randomUUID(), List.of(playerId), 3, 30));

        then(gameProjections).should(never()).save(any());
        then(playerGameStates).should().save(existingPlayerState);
        then(gamePlayers).should().save(gameId, existingPlayer);
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
    void applyPlayerJammed_setsJamFieldsFromPayload() {
        var playerId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of())));

        applier.applyPlayerJammed(new PlayerJammedPayload(gameId, 2, playerId, 3));

        var captor = ArgumentCaptor.forClass(PlayerGameState.class);
        then(playerGameStates).should().save(captor.capture());
        assertThat(captor.getValue().jammedEraNumber()).isEqualTo(2);
        assertThat(captor.getValue().jammedUntilRound()).isEqualTo(3);
    }

    // IncompleteEventPublicationResubmitter can resubmit a failed send out of original order, so a stale
    // era-1 PlayerJammed arriving after an era-2 one was already recorded is a real possibility — it must not
    // revert the player's already-superseded jam back to the earlier era's values.
    @Test
    void applyPlayerJammed_arrivingAfterANewerEraJam_isIgnored() {
        var playerId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(), null, 2, 3)));

        applier.applyPlayerJammed(new PlayerJammedPayload(gameId, 1, playerId, 2));

        then(playerGameStates).should(never()).save(any());
    }

    @Test
    void applyPlayerJammed_arrivesBeforeGameStarted_createsRowRatherThanDropping() {
        var playerId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        applier.applyPlayerJammed(new PlayerJammedPayload(gameId, 1, playerId, 2));

        var captor = ArgumentCaptor.forClass(PlayerGameState.class);
        then(playerGameStates).should().save(captor.capture());
        assertThat(captor.getValue().jammedEraNumber()).isEqualTo(1);
        assertThat(captor.getValue().jammedUntilRound()).isEqualTo(2);
    }

    // An unrelated per-player event (e.g. a card played the same round) must not silently wipe a jam that
    // hasn't been read yet — every reconstruction site must pass the existing jam fields through.
    @Test
    void applyCardPlayed_preservesAnAlreadyRecordedJam() {
        var playerId = UUID.randomUUID();
        var playedCard = new HandCard(UUID.randomUUID(), "PUSH");
        var existing = new PlayerGameState(gameId, playerId, "ERASERS", List.of(playedCard), null, 1, 2);
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.of(existing));

        applier.applyCardPlayed(new CardPlayedPayload(
                gameId,
                1,
                1,
                playerId,
                playedCard.cardInstanceId(),
                io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardType.PUSH,
                io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardGrade.II,
                null,
                List.of(),
                null,
                null,
                null));

        var captor = ArgumentCaptor.forClass(PlayerGameState.class);
        then(playerGameStates).should().save(captor.capture());
        assertThat(captor.getValue().jammedEraNumber()).isEqualTo(1);
        assertThat(captor.getValue().jammedUntilRound()).isEqualTo(2);
    }

    @Test
    void applyEraStarted_setsEraNumberAndPhase() {
        applier.applyEraStarted(new EraStartedPayload(gameId, 2, List.of(), List.of(UUID.randomUUID())));

        then(gameProjections).should().save(new GameProjection(gameId, 2, Phase.ERA_START));
    }

    @Test
    void applyEraStarted_redeliveredForAnAlreadySupersededEra_isSkipped() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 3, Phase.ACTION_ROUND_1)));

        applier.applyEraStarted(new EraStartedPayload(gameId, 2, List.of(), List.of(UUID.randomUUID())));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyEraStarted_redeliveredMidWayThroughTheSameEra_doesNotRollPhaseBackward() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_3)));

        applier.applyEraStarted(new EraStartedPayload(gameId, 2, List.of(), List.of(UUID.randomUUID())));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyEventsDrawn_forAnEraAlreadyClearedByEraEnded_doesNotRepopulateActiveEvents() {
        var eventId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ERA_END)));
        var payload = new EventsDrawnPayload(
                gameId,
                1,
                List.of(new EventsDrawnFutureEvent(
                        eventId,
                        "Title",
                        List.of(),
                        io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CarryOverState.FRESH)));

        applier.applyEventsDrawn(payload);

        then(gameActiveEvents).should(never()).save(any(), any());
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
        assertThat(captor.getValue().outcomes()).containsExactly(new EventOutcome(outcomeId, "desc"));
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
    void applyHandDealt_keepsFinalHandAndRecordsPendingSelection() {
        var playerId = UUID.randomUUID();
        var eraOneCard = new HandCard(UUID.randomUUID(), "SCAN");
        var eraTwoCardId = UUID.randomUUID();
        var expiresAt = Instant.parse("2026-08-15T00:00:00Z");
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(eraOneCard))));

        applier.applyHandDealt(new HandDealtPayload(
                gameId,
                2,
                playerId,
                expiresAt,
                List.of(new HandDealtCardInstance(eraTwoCardId, CardType.PUSH, CardGrade.III, 1))));

        var captor = ArgumentCaptor.forClass(PlayerGameState.class);
        then(playerGameStates).should().save(captor.capture());
        assertThat(captor.getValue().myHand()).containsExactly(eraOneCard);
        assertThat(captor.getValue().pendingHandSelection())
                .isEqualTo(new PendingHandSelection(
                        List.of(new PendingHandCard(eraTwoCardId, "PUSH", "III", 1)), expiresAt));
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
                Instant.parse("2026-08-15T00:00:00Z"),
                List.of(new HandDealtCardInstance(cardId, CardType.PUSH, CardGrade.II, 1))));

        then(playerGameStates)
                .should()
                .save(new PlayerGameState(
                        gameId,
                        playerId,
                        null,
                        List.of(),
                        new PendingHandSelection(
                                List.of(new PendingHandCard(cardId, "PUSH", "II", 1)),
                                Instant.parse("2026-08-15T00:00:00Z"))));
    }

    @Test
    void applyHandSelected_replacesPendingPoolWithFinalGradedHand() {
        var playerId = UUID.randomUUID();
        var selectedCardId = UUID.randomUUID();
        var pending = new PendingHandSelection(
                List.of(new PendingHandCard(selectedCardId, "PUSH", "III", 1)), Instant.parse("2026-08-15T00:00:00Z"));
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(), pending)));

        applier.applyHandSelected(new HandSelectedPayload(
                gameId,
                1,
                playerId,
                HandSelectionOrigin.PLAYER,
                List.of(new HandDealtCardInstance(selectedCardId, CardType.PUSH, CardGrade.III, 1))));

        then(playerGameStates)
                .should()
                .save(new PlayerGameState(
                        gameId, playerId, "ERASERS", List.of(new HandCard(selectedCardId, "PUSH", "III")), null));
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
        then(revealedProbabilityIntel).should().deleteByGameIdAndEraNumber(gameId, 1);
        then(revealedInfluenceIntel).should().deleteByGameIdAndEraNumber(gameId, 1);
        then(revealedHandCardIntel).should().deleteByGameIdAndEraNumber(gameId, 1);
        then(gameActiveEvents).should().deleteByGameId(gameId);
    }

    @Test
    void applyEraEnded_redeliveredForAnAlreadySupersededEra_isSkipped() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_1)));

        applier.applyEraEnded(new EraEndedPayload(gameId, 1, 0, 2));

        then(gameProjections).should(never()).save(any());
        then(revealedProbabilityIntel).should(never()).deleteByGameIdAndEraNumber(any(), anyInt());
        then(revealedInfluenceIntel).should(never()).deleteByGameIdAndEraNumber(any(), anyInt());
        then(revealedHandCardIntel).should(never()).deleteByGameIdAndEraNumber(any(), anyInt());
        then(gameActiveEvents).should(never()).deleteByGameId(any());
    }

    @Test
    void applyEraEnded_afterGameAlreadyEnded_isSkipped() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.GAME_ENDED)));

        applier.applyEraEnded(new EraEndedPayload(gameId, 1, 0, 2));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyProbabilityStateRevealed_beforeGameStarted_retainsPrivateIntelAgainstAnchor() {
        var viewerId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        var payload = new ProbabilityStateRevealedPayload(
                gameId,
                2,
                1,
                viewerId,
                eventId,
                List.of(new ProbabilityStateRevealedOutcomeState(outcomeId, 45, false, true)));

        applier.applyProbabilityStateRevealed(payload);

        then(revealedProbabilityIntel)
                .should()
                .upsertLatest(new RevealedProbabilityIntel(
                        gameId,
                        viewerId,
                        2,
                        eventId,
                        1,
                        List.of(new RevealedProbabilityOutcome(outcomeId, 45, false, true))));
    }

    @Test
    void applyProbabilityStateRevealed_forPriorEra_skipsIt() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_1)));

        applier.applyProbabilityStateRevealed(
                new ProbabilityStateRevealedPayload(gameId, 1, 2, UUID.randomUUID(), UUID.randomUUID(), List.of()));

        then(revealedProbabilityIntel).should(never()).upsertLatest(any());
    }

    @Test
    void applyProbabilityStateRevealed_afterEraEnd_skipsIt() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ERA_END)));

        applier.applyProbabilityStateRevealed(
                new ProbabilityStateRevealedPayload(gameId, 2, 2, UUID.randomUUID(), UUID.randomUUID(), List.of()));

        then(revealedProbabilityIntel).should(never()).upsertLatest(any());
    }

    @Test
    void applyInfluenceTraced_storesInfluencersForTheTracingViewer() {
        var viewerId = UUID.randomUUID();
        var targetEventId = UUID.randomUUID();
        var influencerOne = UUID.randomUUID();
        var influencerTwo = UUID.randomUUID();

        applier.applyInfluenceTraced(new InfluenceTracedPayload(
                gameId, 1, 2, viewerId, targetEventId, List.of(influencerOne, influencerTwo)));

        then(revealedInfluenceIntel)
                .should()
                .upsertLatest(new RevealedInfluenceIntel(
                        gameId, viewerId, 1, targetEventId, 2, List.of(influencerOne, influencerTwo)));
    }

    @Test
    void applyInfluenceTraced_emptyInfluencerSetIsStoredNotDropped() {
        var viewerId = UUID.randomUUID();
        var targetEventId = UUID.randomUUID();

        applier.applyInfluenceTraced(new InfluenceTracedPayload(gameId, 1, 2, viewerId, targetEventId, List.of()));

        then(revealedInfluenceIntel)
                .should()
                .upsertLatest(new RevealedInfluenceIntel(gameId, viewerId, 1, targetEventId, 2, List.of()));
    }

    @Test
    void applyInfluenceTraced_forPriorEra_skipsIt() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_1)));

        applier.applyInfluenceTraced(
                new InfluenceTracedPayload(gameId, 1, 2, UUID.randomUUID(), UUID.randomUUID(), List.of()));

        then(revealedInfluenceIntel).should(never()).upsertLatest(any());
    }

    @Test
    void applyInfluenceTraced_afterEraEnd_skipsIt() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ERA_END)));

        applier.applyInfluenceTraced(
                new InfluenceTracedPayload(gameId, 2, 2, UUID.randomUUID(), UUID.randomUUID(), List.of()));

        then(revealedInfluenceIntel).should(never()).upsertLatest(any());
    }

    @Test
    void applyHandCardIntercepted_storesCardsForTheInterceptingViewerOnly() {
        var viewerId = UUID.randomUUID();
        var targetPlayerId = UUID.randomUUID();
        var cardInstanceId = UUID.randomUUID();

        applier.applyHandCardIntercepted(new HandCardInterceptedPayload(
                gameId,
                1,
                2,
                viewerId,
                targetPlayerId,
                List.of(new InterceptedHandCard(
                        cardInstanceId,
                        io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardType.SWING,
                        io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardGrade.III))));

        then(revealedHandCardIntel)
                .should()
                .upsertLatest(new RevealedHandCardIntel(
                        gameId,
                        viewerId,
                        1,
                        targetPlayerId,
                        2,
                        List.of(new RevealedHandCard(cardInstanceId, "SWING", "III"))));
        then(playerGameStates).shouldHaveNoInteractions();
    }

    @Test
    void applyHandCardIntercepted_emptyRevealIsStoredNotDropped() {
        var viewerId = UUID.randomUUID();
        var targetPlayerId = UUID.randomUUID();

        applier.applyHandCardIntercepted(
                new HandCardInterceptedPayload(gameId, 1, 1, viewerId, targetPlayerId, List.of()));

        then(revealedHandCardIntel)
                .should()
                .upsertLatest(new RevealedHandCardIntel(gameId, viewerId, 1, targetPlayerId, 1, List.of()));
    }

    @Test
    void applyHandCardIntercepted_forPriorEra_skipsIt() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_1)));

        applier.applyHandCardIntercepted(
                new HandCardInterceptedPayload(gameId, 1, 2, UUID.randomUUID(), UUID.randomUUID(), List.of()));

        then(revealedHandCardIntel).should(never()).upsertLatest(any());
    }

    @Test
    void applyHandCardIntercepted_afterEraEnd_skipsIt() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ERA_END)));

        applier.applyHandCardIntercepted(
                new HandCardInterceptedPayload(gameId, 2, 2, UUID.randomUUID(), UUID.randomUUID(), List.of()));

        then(revealedHandCardIntel).should(never()).upsertLatest(any());
    }

    @Test
    void applyGameEnded_preservesEraNumberAndUpdatesScores() {
        var playerId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 3, Phase.RESOLUTION)));
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new GamePlayer(playerId, 10, true, "ERASERS")));

        applier.applyGameEnded(new GameEndedPayload(
                gameId, "SCORE_THRESHOLD", List.of(new GameEndedPlayerScoreResult(playerId, Faction.ERASERS, 20))));

        then(gameProjections).should().save(new GameProjection(gameId, 3, Phase.GAME_ENDED));
        then(gamePlayers).should().save(gameId, new GamePlayer(playerId, 20, true, "ERASERS"));
        then(revealedProbabilityIntel).should().deleteByGameId(gameId);
        then(revealedInfluenceIntel).should().deleteByGameId(gameId);
        then(revealedHandCardIntel).should().deleteByGameId(gameId);
        then(gameActiveEvents).should().deleteByGameId(gameId);
    }

    @Test
    void applyGameEnded_withLaggingProjection_clearsIntelFromAllErasNotJustTheKnownOne() {
        // An out-of-order future-era reveal can be persisted while the projection still
        // lags on an earlier era; the terminal clear must not orphan that row.
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));

        applier.applyGameEnded(new GameEndedPayload(gameId, "SCORE_THRESHOLD", List.of()));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.GAME_ENDED));
        then(revealedProbabilityIntel).should().deleteByGameId(gameId);
        then(revealedInfluenceIntel).should().deleteByGameId(gameId);
        then(revealedHandCardIntel).should().deleteByGameId(gameId);
        then(revealedProbabilityIntel).should(never()).deleteByGameIdAndEraNumber(any(), anyInt());
        then(revealedInfluenceIntel).should(never()).deleteByGameIdAndEraNumber(any(), anyInt());
        then(revealedHandCardIntel).should(never()).deleteByGameIdAndEraNumber(any(), anyInt());
    }

    @Test
    void applyGameEnded_alreadyApplied_isSkipped() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 3, Phase.GAME_ENDED)));

        applier.applyGameEnded(new GameEndedPayload(gameId, "SCORE_THRESHOLD", List.of()));

        then(gameProjections).should(never()).save(any());
        then(revealedProbabilityIntel).should(never()).deleteByGameId(any());
        then(revealedInfluenceIntel).should(never()).deleteByGameId(any());
        then(revealedHandCardIntel).should(never()).deleteByGameId(any());
        then(gameActiveEvents).should(never()).deleteByGameId(any());
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
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_3)));

        applier.applyResolutionStarted(new ResolutionStartedPayload(gameId, 1));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.RESOLUTION));
    }

    @Test
    void applyResolutionStarted_forPastEra_isSkipped() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_1)));

        applier.applyResolutionStarted(new ResolutionStartedPayload(gameId, 1));

        then(gameProjections).should(never()).save(any());
    }

    // A redelivered/duplicate ResolutionStarted arriving once paradox resolution has already begun for the
    // same era must not silently flip the phase back to RESOLUTION — that would desync it from the
    // still-non-empty pendingParadoxIds it was preserving.
    @Test
    void applyResolutionStarted_arrivingDuringParadoxResolution_doesNotRollPhaseBackward() {
        var paradoxId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradoxId))));

        applier.applyResolutionStarted(new ResolutionStartedPayload(gameId, 1));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyActionRoundStarted_mapsRoundNumberToPhase() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));

        applier.applyActionRoundStarted(new ActionRoundStartedPayload(gameId, 1, 2, 45, List.of()));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.ACTION_ROUND_2));
    }

    @Test
    void applyActionRoundStarted_arrivingAfterGameEnded_isSkipped() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.GAME_ENDED)));

        applier.applyActionRoundStarted(new ActionRoundStartedPayload(gameId, 2, 1, 45, List.of()));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyActionRoundStarted_arrivingAfterResolutionStarted_doesNotRollPhaseBackward() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.RESOLUTION)));

        applier.applyActionRoundStarted(new ActionRoundStartedPayload(gameId, 1, 1, 45, List.of()));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyActionRoundStarted_unsupportedRoundNumber_throws() {
        var payload = new ActionRoundStartedPayload(gameId, 1, 4, 45, List.of());

        assertThatThrownBy(() -> applier.applyActionRoundStarted(payload)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyCardPlayed_removesPlayedMultiTargetCardFromHandWithoutPersistingTargets() {
        var playerId = UUID.randomUUID();
        var playedCard = new HandCard(UUID.randomUUID(), "SCAN");
        var otherCard = new HandCard(UUID.randomUUID(), "PUSH");
        var targetEventIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(
                        Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(playedCard, otherCard))));

        applier.applyCardPlayed(new CardPlayedPayload(
                gameId,
                1,
                1,
                playerId,
                playedCard.cardInstanceId(),
                io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardType.SCAN,
                io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardGrade.II,
                null,
                targetEventIds,
                null,
                null,
                null));

        var captor = ArgumentCaptor.forClass(PlayerGameState.class);
        then(playerGameStates).should().save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new PlayerGameState(gameId, playerId, "ERASERS", List.of(otherCard)));
    }

    @Test
    void applyRoundSummaryPublished_replacesThePriorGameWideSummaryWithPublicFieldsOnly() {
        var playerId = UUID.randomUUID();
        var previous = new LastRoundSummary(
                1, 1, List.of(new RoundActionSummary(playerId, "PROBABILITY_SHIFTER", "CARD", false)));
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_2, List.of(), previous)));
        var payload = new RoundSummaryPublishedPayload(
                gameId,
                1,
                2,
                List.of(new io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActionSummary(
                        playerId, "INFORMATION", "CARD", true)));

        applier.applyRoundSummaryPublished(payload);

        then(gameProjections)
                .should()
                .save(new GameProjection(
                        gameId,
                        1,
                        Phase.ACTION_ROUND_2,
                        List.of(),
                        new LastRoundSummary(
                                1, 2, List.of(new RoundActionSummary(playerId, "INFORMATION", "CARD", true)))));
    }

    @Test
    void applyRoundSummaryPublished_doesNotReplaceANewerSummary() {
        var playerId = UUID.randomUUID();
        var current =
                new LastRoundSummary(2, 1, List.of(new RoundActionSummary(playerId, "INFORMATION", "CARD", false)));
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_2, List.of(), current)));

        applier.applyRoundSummaryPublished(new RoundSummaryPublishedPayload(
                gameId,
                1,
                3,
                List.of(new io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActionSummary(
                        playerId, "PROBABILITY_SHIFTER", "CARD", false))));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyRoundSummaryPublished_doesNotUpdateAnEndedGame() {
        var playerId = UUID.randomUUID();
        var current =
                new LastRoundSummary(1, 3, List.of(new RoundActionSummary(playerId, "INFORMATION", "CARD", false)));
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.GAME_ENDED, List.of(), current)));

        applier.applyRoundSummaryPublished(new RoundSummaryPublishedPayload(
                gameId,
                1,
                3,
                List.of(new io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActionSummary(
                        playerId, "INFORMATION", "CARD", false))));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyActionRoundStarted_preservesTheLastRoundSummary() {
        var summary = new LastRoundSummary(
                1, 1, List.of(new RoundActionSummary(UUID.randomUUID(), "INFORMATION", "CARD", false)));
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1, List.of(), summary)));

        applier.applyActionRoundStarted(new ActionRoundStartedPayload(gameId, 1, 2, 45, List.of()));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.ACTION_ROUND_2, List.of(), summary));
    }

    @Test
    void applyScoresUpdated_setsNewTotalPerUpdate() {
        var playerId = UUID.randomUUID();
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new GamePlayer(playerId, 4, true, "PROPHETS")));

        applier.applyScoresUpdated(new ScoresUpdatedPayload(
                gameId,
                1,
                List.of(new ScoreUpdate(
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

        then(gameActiveEvents).should().markResolved(gameId, eventId);
        then(gameActiveEvents).should().deleteByGameIdAndEventId(gameId, eventId);
    }

    @Test
    void applyEventsDrawn_arrivingAfterOutcomeAppliedOnTheOtherTopic_doesNotResurrectTheResolvedEvent() {
        var eventId = UUID.randomUUID();

        // Cross-topic reordering: timeline.events' OutcomeApplied is processed first.
        applier.applyOutcomeApplied(new OutcomeAppliedPayload(gameId, 1, eventId, UUID.randomUUID(), List.of()));
        given(gameActiveEvents.isResolved(gameId, eventId)).willReturn(true);

        // The late game.events' EventsDrawn for the same event arrives afterward.
        applier.applyEventsDrawn(new EventsDrawnPayload(
                gameId,
                1,
                List.of(new EventsDrawnFutureEvent(
                        eventId,
                        "Title",
                        List.of(),
                        io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CarryOverState.FRESH))));

        then(gameActiveEvents).should(never()).save(eq(gameId), any());
    }

    @Test
    void applyParadoxResolutionPhaseStarted_opensPhaseWithPendingParadoxIds() {
        var paradox1 = UUID.randomUUID();
        var paradox2 = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
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
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(
                        new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradox1, paradox2))));

        applier.applyParadoxResolved(new ParadoxResolvedPayload(gameId, 1, paradox1, UUID.randomUUID()));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradox2)));
    }

    @Test
    void applyParadoxCascaded_lastPending_closesPhaseBackToResolution() {
        var paradoxId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradoxId))));

        applier.applyParadoxCascaded(
                new ParadoxCascadedPayload(gameId, 1, paradoxId, UUID.randomUUID(), List.of(), null));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.RESOLUTION, List.of()));
    }

    @Test
    void applyParadoxResolved_mixedWithCascade_closesOnceBothTerminal() {
        var paradox1 = UUID.randomUUID();
        var paradox2 = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(
                        new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradox1, paradox2))));

        applier.applyParadoxResolved(new ParadoxResolvedPayload(gameId, 1, paradox1, UUID.randomUUID()));

        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradox2))));

        applier.applyParadoxCascaded(
                new ParadoxCascadedPayload(gameId, 1, paradox2, UUID.randomUUID(), List.of(), null));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.RESOLUTION, List.of()));
    }

    @Test
    void applyParadoxResolved_notPending_isNoOp() {
        var paradoxId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.RESOLUTION, List.of())));

        applier.applyParadoxResolved(new ParadoxResolvedPayload(gameId, 1, paradoxId, UUID.randomUUID()));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyParadoxResolutionPhaseStarted_beforeGameStarted_appliesAgainstEnsuredAnchor() {
        var paradoxId = UUID.randomUUID();

        applier.applyParadoxResolutionPhaseStarted(
                new ParadoxResolutionPhaseStartedPayload(gameId, 1, List.of(paradoxId), 60));

        then(gameProjections)
                .should()
                .save(new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradoxId)));
    }

    @Test
    void applyParadoxResolved_beforeGameStarted_closesAgainstEnsuredAnchorWithoutRetry() {
        var paradoxId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(paradoxId))));

        applier.applyParadoxResolved(new ParadoxResolvedPayload(gameId, 1, paradoxId, UUID.randomUUID()));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.RESOLUTION, List.of()));
    }

    @Test
    void applyEventsDrawn_forFutureEra_advancesProjectionBeforeExposingActiveEvents() {
        var eventId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ERA_END)));

        applier.applyEventsDrawn(new EventsDrawnPayload(
                gameId,
                2,
                List.of(new EventsDrawnFutureEvent(
                        eventId,
                        "Next-era event",
                        List.of(),
                        io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CarryOverState.FRESH))));

        then(gameProjections).should().save(new GameProjection(gameId, 2, Phase.ERA_START));
        then(gameActiveEvents).should().save(eq(gameId), any());
    }

    @Test
    void applyParadoxResolutionPhaseStarted_forAFutureEra_savesThatFutureEraNumber() {
        var paradoxId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));

        applier.applyParadoxResolutionPhaseStarted(
                new ParadoxResolutionPhaseStartedPayload(gameId, 2, List.of(paradoxId), 60));

        then(gameProjections)
                .should()
                .save(new GameProjection(gameId, 2, Phase.PARADOX_RESOLUTION, List.of(paradoxId)));
    }

    @Test
    void applyParadoxResolutionPhaseStarted_arrivingAfterEraEnd_doesNotReopenTerminalPhase() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ERA_END)));

        applier.applyParadoxResolutionPhaseStarted(
                new ParadoxResolutionPhaseStartedPayload(gameId, 1, List.of(UUID.randomUUID()), 60));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyParadoxResolutionPhaseStarted_forPastEra_isSkipped() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_1)));

        applier.applyParadoxResolutionPhaseStarted(
                new ParadoxResolutionPhaseStartedPayload(gameId, 1, List.of(UUID.randomUUID()), 60));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyParadoxResolved_delayedPastGameEnded_doesNotReopenTerminalPhase() {
        var paradoxId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.GAME_ENDED, List.of(paradoxId))));

        applier.applyParadoxResolved(new ParadoxResolvedPayload(gameId, 1, paradoxId, UUID.randomUUID()));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyParadoxCascaded_delayedPastEraEnd_doesNotReopenTerminalPhase() {
        var paradoxId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ERA_END, List.of(paradoxId))));

        applier.applyParadoxCascaded(
                new ParadoxCascadedPayload(gameId, 1, paradoxId, UUID.randomUUID(), List.of(), null));

        then(gameProjections).should(never()).save(any());
    }
}
