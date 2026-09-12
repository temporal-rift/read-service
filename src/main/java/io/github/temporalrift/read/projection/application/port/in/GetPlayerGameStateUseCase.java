package io.github.temporalrift.read.projection.application.port.in;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.model.GameChain;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.HandCard;
import io.github.temporalrift.read.projection.domain.model.LastRoundSummary;
import io.github.temporalrift.read.projection.domain.model.PendingHandSelection;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.model.RevealedIntelEntry;

public interface GetPlayerGameStateUseCase {

    /** @throws io.github.temporalrift.read.projection.domain.model.PlayerNotInGameException if not a participant */
    Result get(UUID gameId, UUID playerId);

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
            GameChain chain) {

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
    }
}
