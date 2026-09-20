package io.github.temporalrift.read.projection.application.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.PlayerNotInGameException;
import io.github.temporalrift.read.projection.domain.model.RevealedIntelEntry;
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

@Service
class GetPlayerGameStateQueryHandler implements GetPlayerGameStateUseCase {

    private final GameProjectionRepository gameProjections;
    private final GamePlayerRepository gamePlayers;
    private final GameActiveEventRepository gameActiveEvents;
    private final PlayerGameStateRepository playerGameStates;
    private final RevealedProbabilityIntelRepository revealedProbabilityIntel;
    private final RevealedInfluenceIntelRepository revealedInfluenceIntel;
    private final RevealedHandCardIntelRepository revealedHandCardIntel;
    private final GameChainRepository gameChains;
    private final PublicBandRepository publicBands;
    private final PublicDeclarationRepository publicDeclarations;
    private final ExposeFactRepository exposeFacts;
    private final PlayerSubmissionRepository playerSubmissions;
    private final TerminalResultRepository terminalResults;

    GetPlayerGameStateQueryHandler(
            GameProjectionRepository gameProjections,
            GamePlayerRepository gamePlayers,
            GameActiveEventRepository gameActiveEvents,
            PlayerGameStateRepository playerGameStates,
            RevealedProbabilityIntelRepository revealedProbabilityIntel,
            RevealedInfluenceIntelRepository revealedInfluenceIntel,
            RevealedHandCardIntelRepository revealedHandCardIntel,
            GameChainRepository gameChains,
            PublicBandRepository publicBands,
            PublicDeclarationRepository publicDeclarations,
            ExposeFactRepository exposeFacts,
            PlayerSubmissionRepository playerSubmissions,
            TerminalResultRepository terminalResults) {
        this.gameProjections = gameProjections;
        this.gamePlayers = gamePlayers;
        this.gameActiveEvents = gameActiveEvents;
        this.playerGameStates = playerGameStates;
        this.revealedProbabilityIntel = revealedProbabilityIntel;
        this.revealedInfluenceIntel = revealedInfluenceIntel;
        this.revealedHandCardIntel = revealedHandCardIntel;
        this.gameChains = gameChains;
        this.publicBands = publicBands;
        this.publicDeclarations = publicDeclarations;
        this.exposeFacts = exposeFacts;
        this.playerSubmissions = playerSubmissions;
        this.terminalResults = terminalResults;
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
        var eraOver = gameProjection.phase().isEraOver();
        var myRevealedIntel =
                eraOver ? List.<RevealedIntelEntry>of() : combinedIntel(gameId, playerId, gameProjection.eraNumber());
        var myJammedUntilRound =
                playerGameState.effectiveJammedUntilRound(gameProjection.eraNumber(), gameProjection.phase());
        var inActionRound = isActionRound(gameProjection.phase());
        var paradoxOpen = gameProjection.phase() == Phase.PARADOX_RESOLUTION
                && !gameProjection.pendingParadoxIds().isEmpty();

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
                myJammedUntilRound,
                gameChains.findByGameId(gameId).orElse(null),
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
                eraOver ? List.of() : publicBands.findByGameIdAndEraNumber(gameId, gameProjection.eraNumber()),
                eraOver ? List.of() : publicDeclarations.findByGameIdAndEraNumber(gameId, gameProjection.eraNumber()),
                eraOver ? List.of() : exposeFacts.findByGameIdAndEraNumber(gameId, gameProjection.eraNumber()),
                eraOver
                        ? List.of()
                        : playerSubmissions.findByGameIdAndPlayerIdAndEraNumber(
                                gameId, playerId, gameProjection.eraNumber()),
                gameProjection.phase() == Phase.GAME_ENDED
                        ? terminalResults.findByGameId(gameId).orElse(null)
                        : null);
    }

    private static boolean isActionRound(Phase phase) {
        return phase == Phase.ACTION_ROUND_1 || phase == Phase.ACTION_ROUND_2 || phase == Phase.ACTION_ROUND_3;
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
