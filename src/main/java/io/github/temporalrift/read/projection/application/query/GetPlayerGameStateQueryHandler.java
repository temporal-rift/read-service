package io.github.temporalrift.read.projection.application.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.temporalrift.read.projection.application.ProjectionRepositories;
import io.github.temporalrift.read.projection.application.port.in.GetPlayerGameStateUseCase;
import io.github.temporalrift.read.projection.domain.model.DeclarationOffer;
import io.github.temporalrift.read.projection.domain.model.DeclarationWindow;
import io.github.temporalrift.read.projection.domain.model.DetectedParadox;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.GameProjection;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.PlayerNotInGameException;
import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;
import io.github.temporalrift.read.projection.domain.model.ResolutionCardOffer;
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
        var players = stores.gamePlayers().findByGameId(gameId);
        var myScore = players.stream()
                .filter(p -> p.playerId().equals(playerId))
                .findFirst()
                .map(GamePlayer::score)
                .orElse(0);
        var myRevealedIntel = duringOpenEra(
                gameProjection, era -> combinedIntel(gameId, playerId, era), List.<RevealedIntelEntry>of());
        var myJammedUntilRound =
                playerGameState.effectiveJammedUntilRound(gameProjection.eraNumber(), gameProjection.phase());
        var inActionRound = isActionRound(gameProjection.phase());
        var paradoxOpen = gameProjection.phase() == Phase.PARADOX_RESOLUTION
                && !gameProjection.pendingParadoxIds().isEmpty();
        var declarationWindow = openDeclarationWindow(gameProjection);
        var mySubmissions = duringOpenEra(
                gameProjection,
                era -> stores.playerSubmissions().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, era),
                List.<PlayerSubmission>of());

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
                declarationWindow.map(DeclarationWindow::expiresAt).orElse(null),
                paradoxOpen ? gameProjection.paradoxResolutionExpiresAt() : null,
                declarationWindow.isPresent(),
                declarationWindow
                        .map(window -> eligibleDeclarationModes(gameId, playerId, window.eraNumber()))
                        .orElse(null),
                paradoxOpen,
                paradoxOpen ? openParadoxes(gameId, gameProjection.pendingParadoxIds()) : List.of(),
                duringOpenEra(
                        gameProjection, era -> stores.publicBands().findByGameIdAndEraNumber(gameId, era), List.of()),
                duringOpenEra(
                        gameProjection,
                        era -> stores.publicDeclarations().findByGameIdAndEraNumber(gameId, era),
                        List.of()),
                duringOpenEra(
                        gameProjection, era -> stores.exposeFacts().findByGameIdAndEraNumber(gameId, era), List.of()),
                mySubmissions,
                gameProjection.phase() == Phase.GAME_ENDED
                        ? stores.terminalResults().findByGameId(gameId).orElse(null)
                        : null,
                duringOpenEra(
                        gameProjection,
                        era -> stores.foresightPreviews()
                                .findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, era)
                                .orElse(null),
                        null),
                inActionRound
                        ? progress(
                                gameId,
                                gameProjection.eraNumber(),
                                PlayerSubmission.SubmissionWindow.ACTION,
                                gameProjection.currentRoundNumber(),
                                players)
                        : null,
                paradoxOpen
                        ? progress(
                                gameId,
                                gameProjection.eraNumber(),
                                PlayerSubmission.SubmissionWindow.PARADOX_RESOLUTION,
                                null,
                                players)
                        : null,
                paradoxOpen ? gameProjection.affectedEventIds() : List.of(),
                paradoxOpen
                        ? eligibleResolutionCards(gameId, playerId, gameProjection.eraNumber(), mySubmissions)
                                .orElse(null)
                        : null,
                gameProjection.winScoreThreshold());
    }

    /** The event-backed window fact is removed when the declaration window closes. */
    private Optional<DeclarationWindow> openDeclarationWindow(GameProjection projection) {
        return stores.declarationWindows().findByGameIdAndEraNumber(projection.gameId(), projection.eraNumber());
    }

    private List<DeclarationOffer.Mode> eligibleDeclarationModes(UUID gameId, UUID playerId, int eraNumber) {
        return stores.declarationOffers()
                .findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, eraNumber)
                .map(DeclarationOffer::eligibleModes)
                .orElse(List.of());
    }

    /** Empty when no offer is open for the caller; an open offer may still list no cards. */
    private Optional<List<ResolutionCardOffer.Card>> eligibleResolutionCards(
            UUID gameId, UUID playerId, int eraNumber, List<PlayerSubmission> mySubmissions) {
        var alreadyResolved = mySubmissions.stream()
                .anyMatch(submission -> submission.window() == PlayerSubmission.SubmissionWindow.PARADOX_RESOLUTION);
        if (alreadyResolved) {
            return Optional.empty();
        }
        return stores.resolutionCardOffers()
                .findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, eraNumber)
                .map(ResolutionCardOffer::cards);
    }

    /** Pending paradoxes in phase-start order; one without recorded detail is omitted rather than invented. */
    private List<DetectedParadox> openParadoxes(UUID gameId, List<UUID> pendingParadoxIds) {
        var detected = stores.detectedParadoxes().findByGameIdAndParadoxIds(gameId, pendingParadoxIds).stream()
                .collect(Collectors.toMap(DetectedParadox::paradoxId, Function.identity()));
        return pendingParadoxIds.stream()
                .map(detected::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private Result.SubmissionProgress progress(
            UUID gameId,
            int eraNumber,
            PlayerSubmission.SubmissionWindow window,
            Integer roundNumber,
            List<GamePlayer> players) {
        var submitted =
                Set.copyOf(stores.playerSubmissions().findSubmittedPlayerIds(gameId, eraNumber, window, roundNumber));
        var pending = players.stream()
                .map(GamePlayer::playerId)
                .filter(id -> !submitted.contains(id))
                .toList();
        return new Result.SubmissionProgress(players.size() - pending.size(), players.size(), pending);
    }

    /** Era-scoped state is served only while its era is open; an ended era's rows are never read. */
    private static <T> T duringOpenEra(GameProjection projection, IntFunction<T> currentEraRead, T whenEraOver) {
        return projection.phase().isEraOver() ? whenEraOver : currentEraRead.apply(projection.eraNumber());
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
