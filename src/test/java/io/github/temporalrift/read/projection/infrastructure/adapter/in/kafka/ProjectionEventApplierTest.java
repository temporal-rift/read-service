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
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActivistDeclarationMode;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ActivistDeclarationRecordedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.BandedProbabilityEventBandState;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.BandedProbabilityOutcomeBandState;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.BandedProbabilityPublishedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardPlayedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ExposeBehaviorChangedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ExposeInfluenceSignature;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ExposeSignatureRevealedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.HandCardInterceptedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.InfluenceSignatureType;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.InfluenceTracedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.InterceptedHandCard;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ParadoxResolutionCardPlayedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.PlayerJammedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.ProbabilityBand;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.RoundSummaryPublishedPayload;
import io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.SpecialActionPlayedPayload;
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
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.GameStartedPlayer;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandDealtCardInstance;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandDealtPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandSelectedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandSelectionOrigin;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.PlayerDisconnectedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.ResolutionStartedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.TimelineCollapsedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.TimelineStabilizedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.TimelineStabilizedPlayerFactionResult;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.WinConditionMetPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.AdjustedBandsPublishedEventBandState;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.AdjustedBandsPublishedOutcomeBandState;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.AdjustedBandsPublishedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainBrokenPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainCompletedChainLink;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainCompletedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainLinkAddedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ChainReAnchoredPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.OutcomeAppliedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxCascadedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxResolutionPhaseStartedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxResolvedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityStateRevealedOutcomeState;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityStateRevealedPayload;
import io.github.temporalrift.read.projection.application.ProjectionRepositories;
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
import io.github.temporalrift.read.projection.domain.port.out.BandCorrectionRepository;
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

    @Mock
    GameChainRepository gameChains;

    @Mock
    PublicBandRepository publicBands;

    @Mock
    PublicDeclarationRepository publicDeclarations;

    @Mock
    ExposeFactRepository exposeFacts;

    @Mock
    PlayerSubmissionRepository playerSubmissions;

    @Mock
    TerminalResultRepository terminalResults;

    @Mock
    BandCorrectionRepository bandCorrections;

    private ProjectionEventApplier applier;

    private final UUID gameId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient()
                .when(gameProjections.findByGameIdForUpdate(gameId))
                .thenReturn(Optional.of(new GameProjection(gameId, 0, Phase.LOBBY)));
        applier = new ProjectionEventApplier(
                new ProjectionRepositories(
                        gameProjections,
                        gamePlayers,
                        gameActiveEvents,
                        playerGameStates,
                        revealedProbabilityIntel,
                        revealedInfluenceIntel,
                        revealedHandCardIntel,
                        gameChains,
                        publicBands,
                        publicDeclarations,
                        exposeFacts,
                        playerSubmissions,
                        terminalResults),
                bandCorrections);
    }

    @Test
    void applyGameStarted_usesEnsuredProjectionAnchorAndCreatesOneNamedRowPerPlayer() {
        var player1 = UUID.randomUUID();
        var player2 = UUID.randomUUID();
        given(gamePlayers.findByGameIdAndPlayerId(eq(gameId), any())).willReturn(Optional.empty());
        given(playerGameStates.findByGameIdAndPlayerId(eq(gameId), any())).willReturn(Optional.empty());

        applier.applyGameStarted(new GameStartedPayload(
                gameId,
                UUID.randomUUID(),
                List.of(new GameStartedPlayer(player1, "Ada"), new GameStartedPlayer(player2, "Ben")),
                3,
                30));

        then(gameProjections).should(never()).save(any());
        then(gamePlayers).should().save(gameId, new GamePlayer(player1, 0, true, null, "Ada"));
        then(gamePlayers).should().save(gameId, new GamePlayer(player2, 0, true, null, "Ben"));
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

        applier.applyGameStarted(new GameStartedPayload(
                gameId, UUID.randomUUID(), List.of(new GameStartedPlayer(playerId, "Ada")), 3, 30));

        then(gameProjections).should(never()).save(any());
        then(playerGameStates).should().save(existingPlayerState);
        then(gamePlayers).should().save(gameId, existingPlayer.withPlayerName("Ada"));
    }

    @Test
    void applyGameStarted_afterFactionAssignedAlreadyArrived_preservesFactionRatherThanResetting() {
        var playerId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of())));
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        applier.applyGameStarted(new GameStartedPayload(
                gameId, UUID.randomUUID(), List.of(new GameStartedPlayer(playerId, "Ada")), 3, 30));

        then(playerGameStates).should().save(new PlayerGameState(gameId, playerId, "ERASERS", List.of()));
    }

    @Test
    void applyGameStarted_afterHandDealtAlreadyArrived_preservesHandRatherThanResetting() {
        var playerId = UUID.randomUUID();
        var card = new HandCard(UUID.randomUUID(), "PUSH");
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, null, List.of(card))));
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        applier.applyGameStarted(new GameStartedPayload(
                gameId, UUID.randomUUID(), List.of(new GameStartedPlayer(playerId, "Ada")), 3, 30));

        then(playerGameStates).should().save(new PlayerGameState(gameId, playerId, null, List.of(card)));
    }

    @Test
    void applyGameStarted_afterPlayerDisconnectedAlreadyArrived_preservesConnectionStateRatherThanResetting() {
        var playerId = UUID.randomUUID();
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new GamePlayer(playerId, 0, false, null)));
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        applier.applyGameStarted(new GameStartedPayload(
                gameId, UUID.randomUUID(), List.of(new GameStartedPlayer(playerId, "Ada")), 3, 30));

        then(gamePlayers).should().save(gameId, new GamePlayer(playerId, 0, false, null, "Ada"));
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
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));

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
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ERA_START)));

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
        then(gameChains).should().deleteByGameId(gameId);
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
        then(gameChains).should().deleteByGameId(gameId);
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
        then(gameChains).should(never()).deleteByGameId(any());
    }

    @Test
    void applyChainLinkAdded_noExistingChain_startsANewActiveChain() {
        var chainId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));
        given(gameChains.findByGameId(gameId)).willReturn(Optional.empty());

        applier.applyChainLinkAdded(
                new ChainLinkAddedPayload(gameId, chainId, playerId, UUID.randomUUID(), UUID.randomUUID(), 1, null, 1));

        then(gameChains).should().save(gameId, new GameChain(gameId, chainId, ChainStatus.ACTIVE, 1));
    }

    @Test
    void applyChainLinkAdded_growsAnExistingActiveChain() {
        var chainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));
        given(gameChains.findByGameId(gameId))
                .willReturn(Optional.of(new GameChain(gameId, chainId, ChainStatus.ACTIVE, 1)));

        applier.applyChainLinkAdded(new ChainLinkAddedPayload(
                gameId, chainId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2, UUID.randomUUID(), 1));

        then(gameChains).should().save(gameId, new GameChain(gameId, chainId, ChainStatus.ACTIVE, 2));
    }

    @Test
    void applyChainLinkAdded_staleLowerLength_isSkipped() {
        var chainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));
        given(gameChains.findByGameId(gameId))
                .willReturn(Optional.of(new GameChain(gameId, chainId, ChainStatus.ACTIVE, 2)));

        applier.applyChainLinkAdded(new ChainLinkAddedPayload(
                gameId, chainId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2, UUID.randomUUID(), 1));

        then(gameChains).should(never()).save(any(), any());
    }

    @Test
    void applyChainLinkAdded_forAlreadyResolvedChain_isSkipped() {
        var chainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));
        given(gameChains.findByGameId(gameId))
                .willReturn(Optional.of(new GameChain(gameId, chainId, ChainStatus.BROKEN, 2)));

        applier.applyChainLinkAdded(new ChainLinkAddedPayload(
                gameId, chainId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 3, UUID.randomUUID(), 1));

        then(gameChains).should(never()).save(any(), any());
    }

    @Test
    void applyChainLinkAdded_forADifferentChainId_replacesTheResolvedChain() {
        var oldChainId = UUID.randomUUID();
        var newChainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));
        given(gameChains.findByGameId(gameId))
                .willReturn(Optional.of(new GameChain(gameId, oldChainId, ChainStatus.COMPLETED, 3)));

        applier.applyChainLinkAdded(new ChainLinkAddedPayload(
                gameId, newChainId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, null, 1));

        then(gameChains).should().save(gameId, new GameChain(gameId, newChainId, ChainStatus.ACTIVE, 1));
    }

    @Test
    void applyChainLinkAdded_gameAlreadyEnded_isSkipped() {
        var chainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.GAME_ENDED)));

        applier.applyChainLinkAdded(new ChainLinkAddedPayload(
                gameId, chainId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, null, 1));

        then(gameChains).should(never()).findByGameId(any());
        then(gameChains).should(never()).save(any(), any());
    }

    @Test
    void applyChainCompleted_setsCompletedStatusWithLinkCount() {
        var chainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));
        given(gameChains.findByGameId(gameId))
                .willReturn(Optional.of(new GameChain(gameId, chainId, ChainStatus.ACTIVE, 3)));

        applier.applyChainCompleted(new ChainCompletedPayload(
                gameId,
                1,
                chainId,
                UUID.randomUUID(),
                List.of(
                        new ChainCompletedChainLink(UUID.randomUUID(), UUID.randomUUID(), 1),
                        new ChainCompletedChainLink(UUID.randomUUID(), UUID.randomUUID(), 1),
                        new ChainCompletedChainLink(UUID.randomUUID(), UUID.randomUUID(), 1))));

        then(gameChains).should().save(gameId, new GameChain(gameId, chainId, ChainStatus.COMPLETED, 3));
    }

    @Test
    void applyChainCompleted_forAlreadyResolvedChain_isSkipped() {
        var chainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));
        given(gameChains.findByGameId(gameId))
                .willReturn(Optional.of(new GameChain(gameId, chainId, ChainStatus.BROKEN, 2)));

        applier.applyChainCompleted(new ChainCompletedPayload(gameId, 1, chainId, UUID.randomUUID(), List.of()));

        then(gameChains).should(never()).save(any(), any());
    }

    @Test
    void applyChainCompleted_gameAlreadyEnded_isSkipped() {
        var chainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.GAME_ENDED)));

        applier.applyChainCompleted(new ChainCompletedPayload(gameId, 1, chainId, UUID.randomUUID(), List.of()));

        then(gameChains).should(never()).findByGameId(any());
        then(gameChains).should(never()).save(any(), any());
    }

    @Test
    void applyChainBroken_setsBrokenStatusWithLengthAtBreak() {
        var chainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));
        given(gameChains.findByGameId(gameId))
                .willReturn(Optional.of(new GameChain(gameId, chainId, ChainStatus.ACTIVE, 2)));

        applier.applyChainBroken(new ChainBrokenPayload(gameId, 1, chainId, UUID.randomUUID(), UUID.randomUUID(), 2));

        then(gameChains).should().save(gameId, new GameChain(gameId, chainId, ChainStatus.BROKEN, 2));
    }

    @Test
    void applyChainBroken_forAlreadyResolvedChain_isSkipped() {
        var chainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));
        given(gameChains.findByGameId(gameId))
                .willReturn(Optional.of(new GameChain(gameId, chainId, ChainStatus.COMPLETED, 3)));

        applier.applyChainBroken(new ChainBrokenPayload(gameId, 1, chainId, UUID.randomUUID(), UUID.randomUUID(), 3));

        then(gameChains).should(never()).save(any(), any());
    }

    @Test
    void applyChainBroken_gameAlreadyEnded_isSkipped() {
        var chainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.GAME_ENDED)));

        applier.applyChainBroken(new ChainBrokenPayload(gameId, 1, chainId, UUID.randomUUID(), UUID.randomUUID(), 3));

        then(gameChains).should(never()).findByGameId(any());
        then(gameChains).should(never()).save(any(), any());
    }

    @Test
    void applyChainReAnchored_activeChain_keepsLengthAndStaysActive() {
        var chainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));
        given(gameChains.findByGameId(gameId))
                .willReturn(Optional.of(new GameChain(gameId, chainId, ChainStatus.ACTIVE, 2)));

        applier.applyChainReAnchored(new ChainReAnchoredPayload(
                gameId,
                1,
                chainId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                2));

        then(gameChains).should().save(gameId, new GameChain(gameId, chainId, ChainStatus.ACTIVE, 2));
    }

    @Test
    void applyChainReAnchored_forAlreadyResolvedChain_isSkipped() {
        var chainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));
        given(gameChains.findByGameId(gameId))
                .willReturn(Optional.of(new GameChain(gameId, chainId, ChainStatus.BROKEN, 2)));

        applier.applyChainReAnchored(new ChainReAnchoredPayload(
                gameId,
                1,
                chainId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                2));

        then(gameChains).should(never()).save(any(), any());
    }

    @Test
    void applyChainReAnchored_gameAlreadyEnded_isSkipped() {
        var chainId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.GAME_ENDED)));

        applier.applyChainReAnchored(new ChainReAnchoredPayload(
                gameId,
                1,
                chainId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                2));

        then(gameChains).should(never()).findByGameId(any());
        then(gameChains).should(never()).save(any(), any());
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

        applier.applyActionRoundStarted(new ActionRoundStartedPayload(gameId, 1, 2, 45, List.of()), Instant.EPOCH);

        then(gameProjections)
                .should()
                .save(new GameProjection(
                        gameId,
                        1,
                        Phase.ACTION_ROUND_2,
                        List.of(),
                        null,
                        2,
                        Instant.EPOCH.plusSeconds(45),
                        null,
                        0,
                        null));
    }

    @Test
    void applyActionRoundStarted_arrivingAfterGameEnded_isSkipped() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.GAME_ENDED)));

        applier.applyActionRoundStarted(new ActionRoundStartedPayload(gameId, 2, 1, 45, List.of()), Instant.EPOCH);

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyActionRoundStarted_arrivingAfterResolutionStarted_doesNotRollPhaseBackward() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.RESOLUTION)));

        applier.applyActionRoundStarted(new ActionRoundStartedPayload(gameId, 1, 1, 45, List.of()), Instant.EPOCH);

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyActionRoundStarted_unsupportedRoundNumber_throws() {
        var payload = new ActionRoundStartedPayload(gameId, 1, 4, 45, List.of());

        assertThatThrownBy(() -> applier.applyActionRoundStarted(payload, Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class);
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
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_2)));

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

        applier.applyActionRoundStarted(new ActionRoundStartedPayload(gameId, 1, 2, 45, List.of()), Instant.EPOCH);

        then(gameProjections)
                .should()
                .save(new GameProjection(
                        gameId,
                        1,
                        Phase.ACTION_ROUND_2,
                        List.of(),
                        summary,
                        2,
                        Instant.EPOCH.plusSeconds(45),
                        null,
                        0,
                        null));
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
                new ParadoxResolutionPhaseStartedPayload(gameId, 1, List.of(paradox1, paradox2), List.of(), 60),
                Instant.EPOCH);

        then(gameProjections)
                .should()
                .save(new GameProjection(
                        gameId,
                        1,
                        Phase.PARADOX_RESOLUTION,
                        List.of(paradox1, paradox2),
                        null,
                        null,
                        null,
                        Instant.EPOCH.plusSeconds(60),
                        0,
                        null));
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
                new ParadoxCascadedPayload(gameId, 1, paradoxId, null, UUID.randomUUID(), List.of(), null));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.RESOLUTION, List.of()));
    }

    @Test
    void applyParadoxCascaded_multipleFindingsOnOneEvent_closesAllAtOnce() {
        var firstId = UUID.randomUUID();
        var secondId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(
                        new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(firstId, secondId))));

        applier.applyParadoxCascaded(new ParadoxCascadedPayload(
                gameId, 1, firstId, List.of(firstId, secondId), UUID.randomUUID(), List.of(), null));

        then(gameProjections).should().save(new GameProjection(gameId, 1, Phase.RESOLUTION, List.of()));
    }

    @Test
    void applyParadoxCascaded_otherEventAndNewFinding_keepsOnlyOtherEventPending() {
        var firstEventId = UUID.randomUUID();
        var secondEventId = UUID.randomUUID();
        var newlyDetectedId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(
                        new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(firstEventId, secondEventId))));

        applier.applyParadoxCascaded(new ParadoxCascadedPayload(
                gameId, 1, firstEventId, List.of(firstEventId, newlyDetectedId), UUID.randomUUID(), List.of(), null));

        then(gameProjections)
                .should()
                .save(new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(secondEventId)));
    }

    @Test
    void applyParadoxCascaded_replayedAfterClosure_doesNotReopenPhase() {
        var paradoxId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.RESOLUTION, List.of())));

        applier.applyParadoxCascaded(new ParadoxCascadedPayload(
                gameId, 1, paradoxId, List.of(paradoxId), UUID.randomUUID(), List.of(), null));

        then(gameProjections).should(never()).save(any());
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
                new ParadoxCascadedPayload(gameId, 1, paradox2, null, UUID.randomUUID(), List.of(), null));

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
                new ParadoxResolutionPhaseStartedPayload(gameId, 1, List.of(paradoxId), List.of(), 60), Instant.EPOCH);

        then(gameProjections)
                .should()
                .save(new GameProjection(
                        gameId,
                        1,
                        Phase.PARADOX_RESOLUTION,
                        List.of(paradoxId),
                        null,
                        null,
                        null,
                        Instant.EPOCH.plusSeconds(60),
                        0,
                        null));
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
                new ParadoxResolutionPhaseStartedPayload(gameId, 2, List.of(paradoxId), List.of(), 60), Instant.EPOCH);

        then(gameProjections)
                .should()
                .save(new GameProjection(
                        gameId,
                        2,
                        Phase.PARADOX_RESOLUTION,
                        List.of(paradoxId),
                        null,
                        null,
                        null,
                        Instant.EPOCH.plusSeconds(60),
                        0,
                        null));
    }

    @Test
    void applyParadoxResolutionPhaseStarted_arrivingAfterEraEnd_doesNotReopenTerminalPhase() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ERA_END)));

        applier.applyParadoxResolutionPhaseStarted(
                new ParadoxResolutionPhaseStartedPayload(gameId, 1, List.of(UUID.randomUUID()), List.of(), 60),
                Instant.EPOCH);

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyParadoxResolutionPhaseStarted_forPastEra_isSkipped() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_1)));

        applier.applyParadoxResolutionPhaseStarted(
                new ParadoxResolutionPhaseStartedPayload(gameId, 1, List.of(UUID.randomUUID()), List.of(), 60),
                Instant.EPOCH);

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
                new ParadoxCascadedPayload(gameId, 1, paradoxId, null, UUID.randomUUID(), List.of(), null));

        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyBandedProbabilityPublished_storesPreviewWithRoundTwoAge() {
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_3)));

        applier.applyBandedProbabilityPublished(new BandedProbabilityPublishedPayload(
                gameId,
                1,
                List.of(new BandedProbabilityEventBandState(
                        eventId, List.of(new BandedProbabilityOutcomeBandState(outcomeId, ProbabilityBand.HIGH))))));

        var captor = ArgumentCaptor.forClass(List.class);
        then(publicBands).should().replaceAll(eq(gameId), eq(1), captor.capture());
        var bands = (List<PublicBand>) captor.getValue();
        assertThat(bands).hasSize(1);
        assertThat(bands.getFirst().eventId()).isEqualTo(eventId);
        assertThat(bands.getFirst().observedInRound()).isEqualTo(2);
        assertThat(bands.getFirst().outcomes()).containsExactly(new PublicBand.OutcomeBand(outcomeId, "HIGH"));
    }

    @Test
    void applyAdjustedBandsPublished_replacesPreviewForSameGameAndEra() {
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_3)));

        applier.applyAdjustedBandsPublished(new AdjustedBandsPublishedPayload(
                gameId,
                1,
                List.of(new AdjustedBandsPublishedEventBandState(
                        eventId,
                        List.of(new AdjustedBandsPublishedOutcomeBandState(
                                outcomeId,
                                io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ProbabilityBand
                                        .LOW))))));

        var captor = ArgumentCaptor.forClass(List.class);
        then(publicBands).should().replaceAll(eq(gameId), eq(1), captor.capture());
        var bands = (List<PublicBand>) captor.getValue();
        assertThat(bands).hasSize(1);
        assertThat(bands.getFirst().outcomes()).containsExactly(new PublicBand.OutcomeBand(outcomeId, "LOW"));
    }

    @Test
    void applyBandedProbabilityPublished_forStaleEra_isSkipped() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_1)));

        applier.applyBandedProbabilityPublished(new BandedProbabilityPublishedPayload(gameId, 1, List.of()));

        then(publicBands).should(never()).replaceAll(any(), anyInt(), any());
    }

    @Test
    void applyActivistDeclarationRecorded_storesPublicFactAndOwnSubmission() {
        var playerId = UUID.randomUUID();
        var targetEventId = UUID.randomUUID();
        var targetOutcomeId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));

        applier.applyActivistDeclarationRecorded(new ActivistDeclarationRecordedPayload(
                gameId, 1, 1, playerId, ActivistDeclarationMode.RALLY, targetEventId, targetOutcomeId));

        then(publicDeclarations)
                .should()
                .upsert(new PublicDeclaration(gameId, 1, playerId, "RALLY", targetEventId, targetOutcomeId));
        then(playerSubmissions)
                .should()
                .upsert(new PlayerSubmission(
                        gameId, playerId, 1, null, PlayerSubmission.SubmissionKind.DECLARATION, null));
    }

    @Test
    void applyExposeSignatureRevealed_storesRoundTwoFactWithSignature() {
        var activist = UUID.randomUUID();
        var target = UUID.randomUUID();
        var targetEventId = UUID.randomUUID();
        var sourceOutcomeId = UUID.randomUUID();
        var targetOutcomeId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_2)));

        applier.applyExposeSignatureRevealed(new ExposeSignatureRevealedPayload(
                gameId,
                1,
                2,
                activist,
                target,
                new ExposeInfluenceSignature(
                        InfluenceSignatureType.SWING, targetEventId, sourceOutcomeId, targetOutcomeId)));

        then(exposeFacts)
                .should()
                .upsert(new ExposeFact(
                        gameId,
                        1,
                        activist,
                        target,
                        2,
                        "SWING",
                        targetEventId,
                        sourceOutcomeId,
                        targetOutcomeId,
                        false));
    }

    @Test
    void applyExposeBehaviorChanged_storesRoundThreeFactWithoutSignature() {
        var activist = UUID.randomUUID();
        var target = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_3)));

        applier.applyExposeBehaviorChanged(new ExposeBehaviorChangedPayload(gameId, 1, 3, activist, target));

        then(exposeFacts).should().upsert(new ExposeFact(gameId, 1, activist, target, 3, null, null, null, null, true));
    }

    @Test
    void applySpecialActionPlayed_recordsOwnSpecialSubmission() {
        var playerId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));

        applier.applySpecialActionPlayed(new SpecialActionPlayedPayload(
                gameId,
                1,
                1,
                playerId,
                io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.Faction.ERASERS,
                io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.SpecialAction.ANNIHILATE,
                null,
                null,
                null,
                null,
                null));

        then(playerSubmissions)
                .should()
                .upsert(new PlayerSubmission(
                        gameId, playerId, 1, 1, PlayerSubmission.SubmissionKind.ACTION, "SPECIAL"));
    }

    @Test
    void applyParadoxResolutionCardPlayed_recordsOwnParadoxSubmission() {
        var playerId = UUID.randomUUID();
        var cardInstanceId = UUID.randomUUID();
        var targetEventId = UUID.randomUUID();
        var targetOutcomeId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(
                        new GameProjection(gameId, 1, Phase.PARADOX_RESOLUTION, List.of(UUID.randomUUID()))));

        applier.applyParadoxResolutionCardPlayed(new ParadoxResolutionCardPlayedPayload(
                gameId,
                1,
                playerId,
                cardInstanceId,
                io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardType.STABILIZE,
                io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardGrade.I,
                targetEventId,
                targetOutcomeId));

        then(playerSubmissions)
                .should()
                .upsert(new PlayerSubmission(
                        gameId, playerId, 1, null, PlayerSubmission.SubmissionKind.PARADOX_CARD, null));
    }

    @Test
    void applyHandSelected_recordsOwnHandSelectionSubmission() {
        var playerId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of())));
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ERA_START)));

        applier.applyHandSelected(new HandSelectedPayload(
                gameId,
                1,
                playerId,
                HandSelectionOrigin.PLAYER,
                List.of(
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.PUSH,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                1),
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.SCAN,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                2),
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.TRACE,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                3),
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.JAM,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                4),
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.STALL,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                5))));

        then(playerSubmissions)
                .should()
                .upsert(new PlayerSubmission(
                        gameId, playerId, 1, null, PlayerSubmission.SubmissionKind.HAND_SELECTION, null));
    }

    @Test
    void applyHandSelected_timeoutRandom_replacesHandButSkipsSubmission() {
        var playerId = UUID.randomUUID();
        var selectedCardId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of())));
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ERA_START)));

        applier.applyHandSelected(new HandSelectedPayload(
                gameId,
                1,
                playerId,
                HandSelectionOrigin.TIMEOUT_RANDOM,
                List.of(
                        new HandDealtCardInstance(
                                selectedCardId,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.PUSH,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                1),
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.SCAN,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                2),
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.TRACE,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                3),
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.JAM,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                4),
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.STALL,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                5))));

        then(playerGameStates).should().save(any(PlayerGameState.class));
        then(playerSubmissions).should(never()).upsert(any());
    }

    @Test
    void applyHandSelected_forStaleEra_isSkippedEntirely() {
        var playerId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_1)));

        applier.applyHandSelected(new HandSelectedPayload(
                gameId,
                1,
                playerId,
                HandSelectionOrigin.PLAYER,
                List.of(
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.PUSH,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                1),
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.SCAN,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                2),
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.TRACE,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                3),
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.JAM,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                4),
                        new HandDealtCardInstance(
                                UUID.randomUUID(),
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType.STALL,
                                io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade.I,
                                5))));

        then(playerGameStates).shouldHaveNoInteractions();
        then(playerSubmissions).should(never()).upsert(any());
        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyEraEnded_clearsRecoverableEraState() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.RESOLUTION)));

        applier.applyEraEnded(new EraEndedPayload(gameId, 1, 0, 2));

        then(publicBands).should().deleteByGameIdAndEraNumber(gameId, 1);
        then(bandCorrections).should().deleteByGameIdAndEraNumber(gameId, 1);
        then(publicDeclarations).should().deleteByGameIdAndEraNumber(gameId, 1);
        then(exposeFacts).should().deleteByGameIdAndEraNumber(gameId, 1);
        then(playerSubmissions).should().deleteByGameIdAndEraNumber(gameId, 1);
    }

    @Test
    void applyGameEnded_recordsTerminalReasonAndScores() {
        var playerId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.RESOLUTION)));

        applier.applyGameEnded(new GameEndedPayload(
                gameId, "SCORE_THRESHOLD", List.of(new GameEndedPlayerScoreResult(playerId, Faction.WEAVERS, 20))));

        then(terminalResults)
                .should()
                .saveEndReasonAndScores(
                        eq(gameId),
                        eq("SCORE_THRESHOLD"),
                        eq(List.of(new TerminalResult.TerminalScore(playerId, "WEAVERS", 20))));
        then(publicBands).should().deleteByGameId(gameId);
        then(bandCorrections).should().deleteByGameId(gameId);
        then(playerSubmissions).should().deleteByGameId(gameId);
    }

    @Test
    void applyWinConditionMet_addsWinnerAndAdvancesRevision() {
        var winnerId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));

        applier.applyWinConditionMet(
                new WinConditionMetPayload(gameId, winnerId, Faction.PROPHETS, 20, "SCORE_THRESHOLD"));

        then(terminalResults)
                .should()
                .addWinners(
                        eq(gameId),
                        eq(List.of(new TerminalResult.TerminalWinner(winnerId, "PROPHETS", "SCORE_THRESHOLD"))));
        then(gameProjections).should().save(any(GameProjection.class));
    }

    @Test
    void applyBandedProbabilityPublished_afterCorrection_isSkipped() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_3)));
        given(bandCorrections.isCorrectionApplied(gameId, 1)).willReturn(true);

        applier.applyBandedProbabilityPublished(new BandedProbabilityPublishedPayload(gameId, 1, List.of()));

        then(publicBands).should(never()).replaceAll(any(), anyInt(), any());
        then(gameProjections).should(never()).save(any());
    }

    @Test
    void applyAdjustedBandsPublished_marksCorrectionApplied() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_3)));

        applier.applyAdjustedBandsPublished(new AdjustedBandsPublishedPayload(gameId, 1, List.of()));

        then(bandCorrections).should().markCorrectionApplied(gameId, 1);
    }

    @Test
    void applyCardPlayed_forStaleEra_removesHandButSkipsSubmission() {
        var playerId = UUID.randomUUID();
        var playedCard = new HandCard(UUID.randomUUID(), "PUSH");
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(playedCard))));
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_1)));

        applier.applyCardPlayed(new CardPlayedPayload(
                gameId,
                1,
                1,
                playerId,
                playedCard.cardInstanceId(),
                io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardType.PUSH,
                io.github.temporalrift.asyncapi.actionevents.GeneratedChannelContract.CardGrade.I,
                null,
                null,
                null,
                null,
                null));

        then(playerSubmissions).should(never()).upsert(any());
        then(playerGameStates).should().save(any(PlayerGameState.class));
    }

    @Test
    void applyTimelineStabilized_addsWinnersAndAdvancesRevision() {
        var winnerId = UUID.randomUUID();
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 1, Phase.ACTION_ROUND_1)));

        applier.applyTimelineStabilized(new TimelineStabilizedPayload(
                gameId, List.of(new TimelineStabilizedPlayerFactionResult(winnerId, Faction.PROPHETS, 2)), List.of()));

        then(terminalResults)
                .should()
                .addWinners(eq(gameId), eq(List.of(new TerminalResult.TerminalWinner(winnerId, "PROPHETS"))));
        then(gameProjections).should().save(any(GameProjection.class));
    }

    @Test
    void applyTimelineCollapsed_forStaleEra_isSkipped() {
        given(gameProjections.findByGameIdForUpdate(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_1)));

        applier.applyTimelineCollapsed(new TimelineCollapsedPayload(gameId, 1, List.of(), List.of()));

        then(terminalResults).should(never()).addWinners(any(), any());
    }
}
