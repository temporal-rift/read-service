package io.github.temporalrift.read.projection.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.read.projection.application.ProjectionRepositories;
import io.github.temporalrift.read.projection.domain.model.ChainStatus;
import io.github.temporalrift.read.projection.domain.model.ForesightPreview;
import io.github.temporalrift.read.projection.domain.model.ForesightPreviewEvent;
import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.model.GameChain;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.GameProjection;
import io.github.temporalrift.read.projection.domain.model.HandCard;
import io.github.temporalrift.read.projection.domain.model.LastRoundSummary;
import io.github.temporalrift.read.projection.domain.model.PendingHandCard;
import io.github.temporalrift.read.projection.domain.model.PendingHandSelection;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.PlayerGameState;
import io.github.temporalrift.read.projection.domain.model.PlayerNotInGameException;
import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;
import io.github.temporalrift.read.projection.domain.model.PublicBand;
import io.github.temporalrift.read.projection.domain.model.RevealedHandCard;
import io.github.temporalrift.read.projection.domain.model.RevealedHandCardIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedInfluenceIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityIntel;
import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityOutcome;
import io.github.temporalrift.read.projection.domain.model.RoundActionSummary;
import io.github.temporalrift.read.projection.domain.model.TerminalResult;
import io.github.temporalrift.read.projection.domain.port.out.ExposeFactRepository;
import io.github.temporalrift.read.projection.domain.port.out.ForesightPreviewRepository;
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

    @Mock
    ForesightPreviewRepository foresightPreviews;

    @Mock
    GameChainRepository gameChains;

    @Mock
    PublicBandRepository publicBands;

    @Mock
    PublicDeclarationRepository publicDeclarations;

    @Mock
    ExposeFactRepository exposeFacts;

    @Mock
    PlayerSubmissionRepository playerSubmissions;

    @Mock
    TerminalResultRepository terminalResults;

    private GetPlayerGameStateQueryHandler handler;

    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new GetPlayerGameStateQueryHandler(new ProjectionRepositories(
                gameProjections,
                gamePlayers,
                gameActiveEvents,
                playerGameStates,
                revealedProbabilityIntel,
                revealedInfluenceIntel,
                revealedHandCardIntel,
                foresightPreviews,
                gameChains,
                publicBands,
                publicDeclarations,
                exposeFacts,
                playerSubmissions,
                terminalResults));
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
        assertThat(result.chain()).isNull();
    }

    @Test
    void get_participantIncludesTheGameWideChainState() {
        var chainId = UUID.randomUUID();
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, null, List.of())));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_1)));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());
        given(revealedProbabilityIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());
        given(revealedInfluenceIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());
        given(revealedHandCardIntel.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of());
        given(gameChains.findByGameId(gameId))
                .willReturn(Optional.of(new GameChain(gameId, chainId, ChainStatus.ACTIVE, 2)));

        var result = handler.get(gameId, playerId);

        assertThat(result.chain()).isEqualTo(new GameChain(gameId, chainId, ChainStatus.ACTIVE, 2));
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
        then(foresightPreviews).shouldHaveNoInteractions();
        assertThat(result.myForesightPreview()).isNull();
    }

    @Test
    void get_activeEra_includesOnlyTheRequestingPlayersCurrentEraForesightPreview() {
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "PROPHETS", List.of())));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_2)));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());
        var preview = new ForesightPreview(
                gameId, playerId, 2, 3, List.of(new ForesightPreviewEvent(UUID.randomUUID(), "Rise", List.of())), null);
        given(foresightPreviews.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(Optional.of(preview));

        var result = handler.get(gameId, playerId);

        assertThat(result.myForesightPreview()).isEqualTo(preview);
    }

    @Test
    void get_participantWithoutAForesightPreview_hasNone() {
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of())));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ACTION_ROUND_2)));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());
        given(foresightPreviews.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(Optional.empty());

        var result = handler.get(gameId, playerId);

        assertThat(result.myForesightPreview()).isNull();
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

    @Test
    void get_midRound_populatesRecoverableCoordinatesDeadlinesAndPublicState() {
        var roundExpiry = Instant.parse("2026-09-16T00:01:00Z");
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of())));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(
                        gameId,
                        2,
                        Phase.ACTION_ROUND_2,
                        List.of(),
                        null,
                        2,
                        roundExpiry,
                        null,
                        41,
                        Instant.parse("2026-09-16T00:00:00Z"))));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());
        var band = new PublicBand(gameId, 2, UUID.randomUUID(), 2, List.of());
        given(publicBands.findByGameIdAndEraNumber(gameId, 2)).willReturn(List.of(band));
        var submission = new PlayerSubmission(gameId, playerId, 2, 1, PlayerSubmission.SubmissionKind.ACTION, "CARD");
        given(playerSubmissions.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(List.of(submission));

        var result = handler.get(gameId, playerId);

        assertThat(result.revision()).isEqualTo(41);
        assertThat(result.lastUpdatedAt()).isEqualTo(Instant.parse("2026-09-16T00:00:00Z"));
        assertThat(result.roundNumber()).isEqualTo(2);
        assertThat(result.handSelectionExpiresAt()).isNull();
        assertThat(result.actionRoundExpiresAt()).isEqualTo(roundExpiry);
        assertThat(result.paradoxResolutionExpiresAt()).isNull();
        assertThat(result.declarationOpen()).isFalse();
        assertThat(result.paradoxOpen()).isFalse();
        assertThat(result.publicBands()).containsExactly(band);
        assertThat(result.mySubmissions()).containsExactly(submission);
        assertThat(result.terminalResult()).isNull();
    }

    @Test
    void get_outsidePlayablePhases_hidesRoundAndDeadlines() {
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of())));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ERA_START)));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());

        var result = handler.get(gameId, playerId);

        assertThat(result.phase()).isEqualTo(Phase.ERA_START);
        assertThat(result.roundNumber()).isNull();
        assertThat(result.actionRoundExpiresAt()).isNull();
        assertThat(result.paradoxResolutionExpiresAt()).isNull();
        assertThat(result.declarationOpen()).isTrue();
    }

    @Test
    void get_pendingHandSelectionAtEraStart_reportsHandSelectionPhase() {
        var expiresAt = Instant.parse("2026-01-01T00:01:00Z");
        var pending =
                new PendingHandSelection(List.of(new PendingHandCard(UUID.randomUUID(), "PUSH", "I", 1)), expiresAt);
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of(), pending)));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.ERA_START)));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());

        var result = handler.get(gameId, playerId);

        assertThat(result.phase()).isEqualTo(Phase.HAND_SELECTION);
        assertThat(result.pendingHandSelection()).isEqualTo(pending);
        assertThat(result.handSelectionExpiresAt()).isEqualTo(expiresAt);
        assertThat(result.declarationOpen()).isTrue();
    }

    @Test
    void get_endedGame_exposesTerminalResultOnlyThen() {
        var terminal = new TerminalResult(gameId, "SCORE_THRESHOLD", List.of(), List.of());
        given(playerGameStates.findByGameIdAndPlayerId(gameId, playerId))
                .willReturn(Optional.of(new PlayerGameState(gameId, playerId, "ERASERS", List.of())));
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 2, Phase.GAME_ENDED)));
        given(gamePlayers.findByGameId(gameId)).willReturn(List.of());
        given(gameActiveEvents.findByGameId(gameId)).willReturn(List.of());
        given(terminalResults.findByGameId(gameId)).willReturn(Optional.of(terminal));

        var result = handler.get(gameId, playerId);

        assertThat(result.terminalResult()).isEqualTo(terminal);
        assertThat(result.publicBands()).isEmpty();
        assertThat(result.mySubmissions()).isEmpty();
    }
}
