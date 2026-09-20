package io.github.temporalrift.read.projection.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.ExposeFact;
import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.model.GameChain;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.HandCard;
import io.github.temporalrift.read.projection.domain.model.LastRoundSummary;
import io.github.temporalrift.read.projection.domain.model.PendingHandSelection;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.PlayerSubmission;
import io.github.temporalrift.read.projection.domain.model.PublicBand;
import io.github.temporalrift.read.projection.domain.model.PublicDeclaration;
import io.github.temporalrift.read.projection.domain.model.RevealedIntelEntry;
import io.github.temporalrift.read.projection.domain.model.TerminalResult;

public interface GetPlayerGameStateUseCase {

    /** @throws io.github.temporalrift.read.projection.domain.model.PlayerNotInGameException if not a participant */
    Result get(UUID gameId, UUID playerId);

    /**
     * The participant's entitled view. {@code mySpecialBudgets} and {@code myObjectiveProgress} stay
     * absent: no consumed owner fact carries budgets or progress counts, and deriving them would
     * implement write-side game rules. The websocket SNAPSHOT serializes this same record, so REST
     * and notification snapshots share safe semantics by construction.
     */
    record Result(
            UUID gameId,
            int eraNumber,
            Phase phase,
            String myFaction,
            List<HandCard> myHand,
            PendingHandSelection pendingHandSelection,
            List<RevealedIntelEntry> myRevealedIntel,
            int myScore,
            List<GamePlayer> players,
            List<GameActiveEvent> activeEvents,
            LastRoundSummary lastRoundSummary,
            Integer myJammedUntilRound,
            GameChain chain,
            long revision,
            Instant lastUpdatedAt,
            Integer roundNumber,
            Instant handSelectionExpiresAt,
            Instant actionRoundExpiresAt,
            Instant paradoxResolutionExpiresAt,
            boolean declarationOpen,
            boolean paradoxOpen,
            List<UUID> openParadoxIds,
            List<PublicBand> publicBands,
            List<PublicDeclaration> declarations,
            List<ExposeFact> exposeFacts,
            List<PlayerSubmission> mySubmissions,
            TerminalResult terminalResult) {

        public Result(
                UUID gameId,
                int eraNumber,
                Phase phase,
                String myFaction,
                List<HandCard> myHand,
                PendingHandSelection pendingHandSelection,
                List<RevealedIntelEntry> myRevealedIntel,
                int myScore,
                List<GamePlayer> players,
                List<GameActiveEvent> activeEvents) {
            this(
                    gameId,
                    eraNumber,
                    phase,
                    myFaction,
                    myHand,
                    pendingHandSelection,
                    myRevealedIntel,
                    myScore,
                    players,
                    activeEvents,
                    null,
                    null,
                    null);
        }

        public Result(
                UUID gameId,
                int eraNumber,
                Phase phase,
                String myFaction,
                List<HandCard> myHand,
                PendingHandSelection pendingHandSelection,
                List<RevealedIntelEntry> myRevealedIntel,
                int myScore,
                List<GamePlayer> players,
                List<GameActiveEvent> activeEvents,
                LastRoundSummary lastRoundSummary,
                Integer myJammedUntilRound,
                GameChain chain) {
            this(
                    gameId,
                    eraNumber,
                    phase,
                    myFaction,
                    myHand,
                    pendingHandSelection,
                    myRevealedIntel,
                    myScore,
                    players,
                    activeEvents,
                    lastRoundSummary,
                    myJammedUntilRound,
                    chain,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    null);
        }
    }
}
