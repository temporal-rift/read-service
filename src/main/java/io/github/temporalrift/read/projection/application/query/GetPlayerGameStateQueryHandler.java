package io.github.temporalrift.read.projection.application.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.PlayerNotInGameException;
import io.github.temporalrift.read.projection.domain.model.RevealedIntelEntry;
import io.github.temporalrift.read.projection.domain.port.out.GameActiveEventRepository;
import io.github.temporalrift.read.projection.domain.port.out.GamePlayerRepository;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;
import io.github.temporalrift.read.projection.domain.port.out.PlayerGameStateRepository;
import io.github.temporalrift.read.projection.domain.port.out.RevealedHandCardIntelRepository;
import io.github.temporalrift.read.projection.domain.port.out.RevealedInfluenceIntelRepository;
import io.github.temporalrift.read.projection.domain.port.out.RevealedProbabilityIntelRepository;

@Service
class GetPlayerGameStateQueryHandler implements GetPlayerGameStateUseCase {

    private final GameProjectionRepository gameProjections;
    private final GamePlayerRepository gamePlayers;
    private final GameActiveEventRepository gameActiveEvents;
    private final PlayerGameStateRepository playerGameStates;
    private final RevealedProbabilityIntelRepository revealedProbabilityIntel;
    private final RevealedInfluenceIntelRepository revealedInfluenceIntel;
    private final RevealedHandCardIntelRepository revealedHandCardIntel;

    GetPlayerGameStateQueryHandler(
            GameProjectionRepository gameProjections,
            GamePlayerRepository gamePlayers,
            GameActiveEventRepository gameActiveEvents,
            PlayerGameStateRepository playerGameStates,
            RevealedProbabilityIntelRepository revealedProbabilityIntel,
            RevealedInfluenceIntelRepository revealedInfluenceIntel,
            RevealedHandCardIntelRepository revealedHandCardIntel) {
        this.gameProjections = gameProjections;
        this.gamePlayers = gamePlayers;
        this.gameActiveEvents = gameActiveEvents;
        this.playerGameStates = playerGameStates;
        this.revealedProbabilityIntel = revealedProbabilityIntel;
        this.revealedInfluenceIntel = revealedInfluenceIntel;
        this.revealedHandCardIntel = revealedHandCardIntel;
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
        var myRevealedIntel = gameProjection.phase().isEraOver()
                ? List.<RevealedIntelEntry>of()
                : combinedIntel(gameId, playerId, gameProjection.eraNumber());
        var myJammedUntilRound =
                playerGameState.effectiveJammedUntilRound(gameProjection.eraNumber(), gameProjection.phase());

        return new Result(
                gameId,
                gameProjection.eraNumber(),
                gameProjection.phase(),
                playerGameState.myFaction(),
                playerGameState.myHand(),
                playerGameState.pendingHandSelection(),
                myRevealedIntel,
                myScore,
                players,
                gameActiveEvents.findByGameId(gameId),
                gameProjection.lastRoundSummary(),
                myJammedUntilRound);
    }

    private List<RevealedIntelEntry> combinedIntel(UUID gameId, UUID playerId, int eraNumber) {
        var intel = new ArrayList<RevealedIntelEntry>(
                revealedProbabilityIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, eraNumber));
        intel.addAll(revealedInfluenceIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, eraNumber));
        intel.addAll(revealedHandCardIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, eraNumber));
        intel.sort(Comparator.comparing(RevealedIntelEntry::eventId));
        return List.copyOf(intel);
    }
}
