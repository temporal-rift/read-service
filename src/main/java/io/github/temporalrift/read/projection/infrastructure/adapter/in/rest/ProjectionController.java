package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import io.github.temporalrift.read.projection.application.port.in.GetGameHistoryUseCase;
import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.ProjectionApi;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.GameHistoryResponse;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PlayerGameStateResponse;
import io.github.temporalrift.read.shared.CurrentPlayer;

@RestController
class ProjectionController implements ProjectionApi {

    private final GetPlayerGameStateUseCase getPlayerGameStateUseCase;
    private final GetGameHistoryUseCase getGameHistoryUseCase;

    ProjectionController(
            GetPlayerGameStateUseCase getPlayerGameStateUseCase, GetGameHistoryUseCase getGameHistoryUseCase) {
        this.getPlayerGameStateUseCase = getPlayerGameStateUseCase;
        this.getGameHistoryUseCase = getGameHistoryUseCase;
    }

    @Override
    public ResponseEntity<GameHistoryResponse> getGameHistory(UUID gameId) {
        return ResponseEntity.ok(ProjectionRestMapper.toResponse(getGameHistoryUseCase.get(gameId)));
    }

    @Override
    public ResponseEntity<PlayerGameStateResponse> getGameState(UUID gameId) {
        var result = getPlayerGameStateUseCase.get(gameId, CurrentPlayer.id());
        return ResponseEntity.ok(ProjectionRestMapper.toResponse(result));
    }
}
