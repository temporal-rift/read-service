package io.github.temporalrift.read.projection.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.PlayerGameState;
import io.github.temporalrift.read.projection.domain.model.PlayerNotInGameException;
import io.github.temporalrift.read.projection.domain.port.out.GameActiveEventRepository;
import io.github.temporalrift.read.projection.domain.port.out.GamePlayerRepository;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;
import io.github.temporalrift.read.projection.domain.port.out.PlayerGameStateRepository;

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

    private GetPlayerGameStateQueryHandler handler;

    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new GetPlayerGameStateQueryHandler(gameProjections, gamePlayers, gameActiveEvents, playerGameStates);
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
    }

    @Test
    void get_nonParticipant_throwsPlayerNotInGame() {
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> handler.get(gameId, playerId)).isInstanceOf(PlayerNotInGameException.class);
    }
}
