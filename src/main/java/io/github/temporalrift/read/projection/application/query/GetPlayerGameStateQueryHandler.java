package io.github.temporalrift.read.projection.application.query;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.PlayerNotInGameException;
import io.github.temporalrift.read.projection.domain.port.out.GameActiveEventRepository;
import io.github.temporalrift.read.projection.domain.port.out.GamePlayerRepository;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;
import io.github.temporalrift.read.projection.domain.port.out.PlayerGameStateRepository;

@Service
class GetPlayerGameStateQueryHandler implements GetPlayerGameStateUseCase {

    private final GameProjectionRepository gameProjections;
    private final GamePlayerRepository gamePlayers;
    private final GameActiveEventRepository gameActiveEvents;
    private final PlayerGameStateRepository playerGameStates;

    GetPlayerGameStateQueryHandler(
            GameProjectionRepository gameProjections,
            GamePlayerRepository gamePlayers,
            GameActiveEventRepository gameActiveEvents,
            PlayerGameStateRepository playerGameStates) {
        this.gameProjections = gameProjections;
        this.gamePlayers = gamePlayers;
        this.gameActiveEvents = gameActiveEvents;
        this.playerGameStates = playerGameStates;
    }

    @Override
    @Transactional(readOnly = true)
    public Result get(UUID gameId, UUID playerId) {
        var playerGameState = playerGameStates
                .findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new PlayerNotInGameException(gameId, playerId));
        var gameProjection =
                gameProjections.findByGameId(gameId).orElseThrow(() -> new PlayerNotInGameException(gameId, playerId));
        var players = gamePlayers.findByGameId(gameId);
        var myScore = players.stream()
                .filter(p -> p.playerId().equals(playerId))
                .findFirst()
                .map(GamePlayer::score)
                .orElse(0);

        return new Result(
                gameId,
                gameProjection.eraNumber(),
                gameProjection.phase(),
                playerGameState.myFaction(),
                playerGameState.myHand(),
                myScore,
                players,
                gameActiveEvents.findByGameId(gameId));
    }
}
