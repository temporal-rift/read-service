package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
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
    private final int maxEras;

    ProjectionController(
            GetPlayerGameStateUseCase getPlayerGameStateUseCase,
            GetGameHistoryUseCase getGameHistoryUseCase,
            @Value("${game.rules.max-eras}") int maxEras) {
        this.getPlayerGameStateUseCase = getPlayerGameStateUseCase;
        this.getGameHistoryUseCase = getGameHistoryUseCase;
        this.maxEras = maxEras;
    }

    @Override
    public ResponseEntity<GameHistoryResponse> getGameHistory(UUID gameId) {
        var result = getGameHistoryUseCase.get(gameId, CurrentPlayer.id());
        return ResponseEntity.ok(ProjectionRestMapper.toResponse(result));
    }

    @Override
    public ResponseEntity<PlayerGameStateResponse> getGameState(UUID gameId) {
        var result = getPlayerGameStateUseCase.get(gameId, CurrentPlayer.id());
        return ResponseEntity.ok(ProjectionRestMapper.toResponse(result, maxEras));
    }
}
