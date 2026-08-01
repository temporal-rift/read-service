package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        applier.applyGameStarted(new GameStartedPayload(gameId, UUID.randomUUID(), List.of(player1, player2), 3, 30));

        then(gameProjections).should().save(new GameProjection(gameId, 0, Phase.LOBBY));
        then(gamePlayers).should().save(gameId, new GamePlayer(player1, 0, true, null));
        then(gamePlayers).should().save(gameId, new GamePlayer(player2, 0, true, null));
        then(playerGameStates).should().save(new PlayerGameState(gameId, player1, null, List.of()));
        then(playerGameStates).should().save(new PlayerGameState(gameId, player2, null, List.of()));
    }

    @Test
    void applyFactionAssigned_updatesOnlyThatPlayersOwnFaction() {
        var playerId = UUID.randomUUID();
        var existing = new PlayerGameState(gameId, playerId, null, List.of());
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.of(existing));

        applier.applyFactionAssigned(new FactionAssignedPayload(gameId, playerId, "ERASERS"));

        then(playerGameStates).should().save(new PlayerGameState(gameId, playerId, "ERASERS", List.of()));
    }

    @Test
    void applyFactionAssigned_unknownPlayer_skipsWithoutSaving() {
        var playerId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        applier.applyFactionAssigned(new FactionAssignedPayload(gameId, playerId, "ERASERS"));

        then(playerGameStates).should(never()).save(any());
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
                List.of(new EventsDrawnPayload.DrawnEvent(
                        eventId, "Title", List.of(new EventsDrawnPayload.DrawnOutcome(outcomeId, "desc", 50)), false)));

        applier.applyEventsDrawn(payload);

        var captor = ArgumentCaptor.forClass(GameActiveEvent.class);
        then(gameActiveEvents).should().save(eq(gameId), captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo(eventId);
        assertThat(captor.getValue().outcomes())
                .containsExactly(
                        new io.github.temporalrift.read.projection.domain.model.EventOutcome(outcomeId, "desc"));
    }

    @Test
    void applyHandDealt_appendsCardsToExistingHand() {
        var playerId = UUID.randomUUID();
        var existingCard = new HandCard(UUID.randomUUID(), "SCAN");
        var newCardId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(existingCard))));

        applier.applyHandDealt(
                new HandDealtPayload(gameId, 1, playerId, List.of(new HandDealtPayload.DealtCard(newCardId, "PUSH"))));

        var captor = ArgumentCaptor.forClass(PlayerGameState.class);
        then(playerGameStates).should().save(captor.capture());
        assertThat(captor.getValue().myHand()).containsExactly(existingCard, new HandCard(newCardId, "PUSH"));
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
                gameId, "SCORE_THRESHOLD", List.of(new GameEndedPayload.FinalScore(playerId, "ERASERS", 20))));

        then(gameProjections).should().save(new GameProjection(gameId, 3, Phase.GAME_ENDED));
        then(gamePlayers).should().save(gameId, new GamePlayer(playerId, 20, true, "ERASERS"));
    }

    @Test
    void applyFactionRevealed_updatesFactionForEachReveal() {
        var playerId = UUID.randomUUID();
        given(gamePlayers.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new GamePlayer(playerId, 5, true, null)));

        applier.applyFactionRevealed(
                new FactionRevealedPayload(gameId, List.of(new FactionRevealedPayload.Reveal(playerId, "WEAVERS"))));

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
                "PUSH",
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
                gameId, 1, List.of(new ScoresUpdatedPayload.ScoreUpdate(playerId, "PROPHETS", 4, "REASON", 8))));

        then(gamePlayers).should().save(gameId, new GamePlayer(playerId, 8, true, "PROPHETS"));
    }

    @Test
    void applyOutcomeApplied_removesEventFromActiveEvents() {
        var eventId = UUID.randomUUID();

        applier.applyOutcomeApplied(new OutcomeAppliedPayload(gameId, 1, eventId, UUID.randomUUID(), List.of()));

        then(gameActiveEvents).should().deleteByGameIdAndEventId(gameId, eventId);
    }
}
