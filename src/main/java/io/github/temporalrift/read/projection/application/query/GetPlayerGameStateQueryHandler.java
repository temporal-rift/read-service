package io.github.temporalrift.read.projection.application.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.temporalrift.read.projection.application.ProjectionRepositories;
import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.PlayerNotInGameException;
import io.github.temporalrift.read.projection.domain.model.RevealedIntelEntry;

@Service
class GetPlayerGameStateQueryHandler implements GetPlayerGameStateUseCase {

    private final ProjectionRepositories stores;

    GetPlayerGameStateQueryHandler(ProjectionRepositories stores) {
        this.stores = stores;
    }

    @Override
    @Transactional(readOnly = true)
    public Result get(UUID gameId, UUID playerId) {
        var playerGameState = stores.playerGameStates()
                .findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new PlayerNotInGameException(gameId, playerId));
        var gameProjection = stores.gameProjections()
                .findByGameId(gameId)
                .orElseThrow(() -> new PlayerNotInGameException(gameId, playerId));
        var names = stores.playerNames().findByGameId(gameId);
        var players = stores.gamePlayers().findByGameId(gameId).stream()
                .map(player -> player.withPlayerName(names.get(player.playerId())))
                .toList();
        var myScore = players.stream()
                .filter(p -> p.playerId().equals(playerId))
                .findFirst()
                .map(GamePlayer::score)
                .orElse(0);
        var eraOver = gameProjection.phase().isEraOver();
        var myRevealedIntel =
                eraOver ? List.<RevealedIntelEntry>of() : combinedIntel(gameId, playerId, gameProjection.eraNumber());
        var myJammedUntilRound =
                playerGameState.effectiveJammedUntilRound(gameProjection.eraNumber(), gameProjection.phase());
        var inActionRound = isActionRound(gameProjection.phase());
        var paradoxOpen = gameProjection.phase() == Phase.PARADOX_RESOLUTION
                && !gameProjection.pendingParadoxIds().isEmpty();

        // The projection tracks game-wide phase; the hand-selection window is per player.
        var participantPhase =
                gameProjection.phase() == Phase.ERA_START && playerGameState.pendingHandSelection() != null
                        ? Phase.HAND_SELECTION
                        : gameProjection.phase();

        return new Result(
                gameId,
                gameProjection.eraNumber(),
                participantPhase,
                playerGameState.myFaction(),
                playerGameState.myHand(),
                playerGameState.pendingHandSelection(),
                myRevealedIntel,
                myScore,
                players,
                stores.gameActiveEvents().findByGameId(gameId),
                gameProjection.lastRoundSummary(),
                myJammedUntilRound,
                stores.gameChains().findByGameId(gameId).orElse(null),
                gameProjection.revision(),
                gameProjection.lastUpdatedAt(),
                // The paradox phase carries no round of its own; the last open action round stays the
                // coordinate so stale-submission guards keep working until the era closes.
                inActionRound || paradoxOpen ? gameProjection.currentRoundNumber() : null,
                playerGameState.pendingHandSelection() == null
                        ? null
                        : playerGameState.pendingHandSelection().expiresAt(),
                inActionRound ? gameProjection.actionRoundExpiresAt() : null,
                paradoxOpen ? gameProjection.paradoxResolutionExpiresAt() : null,
                // No owner event marks the declaration window; it opens once hands are dealt (ERA_START)
                // and closes when Round 1 starts. This approximation is documented on the response.
                gameProjection.phase() == Phase.ERA_START,
                paradoxOpen,
                paradoxOpen ? gameProjection.pendingParadoxIds() : List.of(),
                eraOver ? List.of() : stores.publicBands().findByGameIdAndEraNumber(gameId, gameProjection.eraNumber()),
                eraOver
                        ? List.of()
                        : stores.publicDeclarations().findByGameIdAndEraNumber(gameId, gameProjection.eraNumber()),
                eraOver ? List.of() : stores.exposeFacts().findByGameIdAndEraNumber(gameId, gameProjection.eraNumber()),
                eraOver
                        ? List.of()
                        : stores.playerSubmissions()
                                .findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, gameProjection.eraNumber()),
                gameProjection.phase() == Phase.GAME_ENDED
                        ? stores.terminalResults().findByGameId(gameId).orElse(null)
                        : null);
    }

    private static boolean isActionRound(Phase phase) {
        return phase == Phase.ACTION_ROUND_1 || phase == Phase.ACTION_ROUND_2 || phase == Phase.ACTION_ROUND_3;
    }

    private List<RevealedIntelEntry> combinedIntel(UUID gameId, UUID playerId, int eraNumber) {
        var intel = new ArrayList<RevealedIntelEntry>(
                stores.revealedProbabilityIntel().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, eraNumber));
        intel.addAll(stores.revealedInfluenceIntel().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, eraNumber));
        intel.addAll(stores.revealedHandCardIntel().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, eraNumber));
        intel.sort(Comparator.comparing(RevealedIntelEntry::eventId));
        return List.copyOf(intel);
    }
}
