package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import io.github.temporalrift.read.projection.application.port.in.GetGameHistoryUseCase;
import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.domain.model.GameProjectionNotReadyException;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.shared.PlayerPrincipal;
import io.github.temporalrift.read.shared.infrastructure.config.PlayerAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class ProjectionControllerTest {

    @Mock
    private GetPlayerGameStateUseCase getPlayerGameStateUseCase;

    @Mock
    private GetGameHistoryUseCase getGameHistoryUseCase;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getGameState_withUnavailableThresholdReturnsRetryableFailure() {
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        authenticate(playerId);
        given(getPlayerGameStateUseCase.get(gameId, playerId)).willReturn(result(gameId, null));
        var controller = new ProjectionController(getPlayerGameStateUseCase, getGameHistoryUseCase, 3);

        assertThatThrownBy(() -> controller.getGameState(gameId))
                .isInstanceOf(GameProjectionNotReadyException.class)
                .hasMessage("Game state is not ready. Retry shortly.");
    }

    @Test
    void getGameState_withThresholdMapsTheParticipantView() {
        var gameId = UUID.randomUUID();
        var playerId = UUID.randomUUID();
        authenticate(playerId);
        given(getPlayerGameStateUseCase.get(gameId, playerId)).willReturn(result(gameId, 20));
        var controller = new ProjectionController(getPlayerGameStateUseCase, getGameHistoryUseCase, 3);

        var response = controller.getGameState(gameId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getWinScoreThreshold()).isEqualTo(20);
    }

    private static GetPlayerGameStateUseCase.Result result(UUID gameId, Integer winScoreThreshold) {
        return new GetPlayerGameStateUseCase.Result(
                gameId, 1, Phase.LOBBY, null, List.of(), null, List.of(), 0, List.of(), List.of(), winScoreThreshold);
    }

    private static void authenticate(UUID playerId) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new PlayerAuthenticationToken(new PlayerPrincipal(playerId)));
        SecurityContextHolder.setContext(context);
    }
}
