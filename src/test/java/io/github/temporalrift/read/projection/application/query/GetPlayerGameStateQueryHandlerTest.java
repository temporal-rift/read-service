package io.github.temporalrift.read.projection.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.GameProjection;
import io.github.temporalrift.read.projection.domain.model.HandCard;
import io.github.temporalrift.read.projection.domain.model.LastRoundSummary;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.PlayerGameState;
import io.github.temporalrift.read.projection.domain.model.PlayerNotInGameException;
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
class GetPlayerGameStateQueryHandlerTest {

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

    private GetPlayerGameStateQueryHandler handler;

    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new GetPlayerGameStateQueryHandler(
                gameProjections,
                gamePlayers,
                gameActiveEvents,
                playerGameStates,
                revealedProbabilityIntel,
                revealedInfluenceIntel,
                revealedHandCardIntel);
    }

    @Test
    void get_participant_composesResultFromAllFourReadModels() {
        var hand = List.of(new HandCard(UUID.randomUUID(), "PUSH"));
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", hand)));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_1)));
        var players = List.of(new GamePlayer(playerId, 7, true, null));
        given(gamePlayers.findByGameId(gameId)).willReturn(players);
        var activeEvents = List.<GameActiveEvent>of();
        given(gameActiveEvents.findByGameId(gameId)).willReturn(activeEvents);

        var result = handler.get(gameId, playerId);

        assertThat(result.gameId()).isEqualTo(gameId);
        assertThat(result.eraNumber()).isEqualTo(2);
        assertThat(result.phase()).isEqualTo(Phase.ACTION_ROUND_1);
        assertThat(result.myFaction()).isEqualTo("ERASERS");
        assertThat(result.myHand()).isEqualTo(hand);
        assertThat(result.myScore()).isEqualTo(7);
        assertThat(result.players()).isEqualTo(players);
        assertThat(result.activeEvents()).isEqualTo(activeEvents);
        assertThat(result.lastRoundSummary()).isNull();
    }

    @Test
    void get_participantIncludesTheGameWideLastRoundSummary() {
        var summary =
                new LastRoundSummary(2, 2, List.of(new RoundActionSummary(playerId, "INFORMATION", "CARD", false)));
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, null, List.of())));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_2, List.of(), summary)));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());
        given(revealedProbabilityIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());
        given(revealedInfluenceIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());
        given(revealedHandCardIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());

        var result = handler.get(gameId, playerId);

        assertThat(result.lastRoundSummary()).isEqualTo(summary);
    }

    @Test
    void get_activeEra_includesOnlyRequestingPlayersCurrentEraIntel() {
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, null, List.of())));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_2)));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());
        var intel = new RevealedProbabilityIntel(
                gameId,
                playerId,
                2,
                UUID.randomUUID(),
                2,
                List.of(new RevealedProbabilityOutcome(UUID.randomUUID(), 50, false, false)));
        given(revealedProbabilityIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of(intel));
        given(revealedInfluenceIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());
        given(revealedHandCardIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());

        var result = handler.get(gameId, playerId);

        assertThat(result.myRevealedIntel()).containsExactly(intel);
    }

    @Test
    void get_activeEra_includesTracedInfluenceAlongsideProbabilityIntel() {
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, null, List.of())));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_2)));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());
        given(revealedProbabilityIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());
        var influence =
                new RevealedInfluenceIntel(gameId, playerId, 2, UUID.randomUUID(), 2, List.of(UUID.randomUUID()));
        given(revealedInfluenceIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of(influence));
        given(revealedHandCardIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());

        var result = handler.get(gameId, playerId);

        assertThat(result.myRevealedIntel()).containsExactly(influence);
    }

    @Test
    void get_activeEra_includesInterceptedHandCardsAlongsideOtherIntel() {
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, null, List.of())));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_2)));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());
        given(revealedProbabilityIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());
        given(revealedInfluenceIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());
        var intercepted = new RevealedHandCardIntel(
                gameId,
                playerId,
                2,
                UUID.randomUUID(),
                1,
                List.of(new RevealedHandCard(UUID.randomUUID(), "SWING", "III")));
        given(revealedHandCardIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of(intercepted));

        var result = handler.get(gameId, playerId);

        assertThat(result.myRevealedIntel()).containsExactly(intercepted);
    }

    @Test
    void get_endedEra_hidesIntelWithoutQueryingTheIntelRepository() {
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, null, List.of())));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ERA_END)));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());

        var result = handler.get(gameId, playerId);

        assertThat(result.myRevealedIntel()).isEmpty();
        then(revealedProbabilityIntel).shouldHaveNoInteractions();
        then(revealedInfluenceIntel).shouldHaveNoInteractions();
        then(revealedHandCardIntel).shouldHaveNoInteractions();
    }

    @Test
    void get_playerCurrentlyJammed_includesTheirJammedUntilRound() {
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(), null, 2, 3)));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_2)));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());
        given(revealedProbabilityIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());
        given(revealedInfluenceIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());
        given(revealedHandCardIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());

        var result = handler.get(gameId, playerId);

        assertThat(result.myJammedUntilRound()).isEqualTo(3);
    }

    @Test
    void get_jamFromAPastRound_isNull() {
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(), null, 2, 1)));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_3)));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());
        given(revealedProbabilityIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());
        given(revealedInfluenceIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());
        given(revealedHandCardIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());

        var result = handler.get(gameId, playerId);

        assertThat(result.myJammedUntilRound()).isNull();
    }

    @Test
    void get_nonParticipant_throwsPlayerNotInGame() {
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> handler.get(gameId, playerId)).isInstanceOf(PlayerNotInGameException.class);
    }
}
