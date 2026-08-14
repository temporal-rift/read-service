package io.github.temporalrift.read.projection.application.port.in;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.model.GamePlayer;
import io.github.temporalrift.read.projection.domain.model.HandCard;
import io.github.temporalrift.read.projection.domain.model.PendingHandSelection;
import io.github.temporalrift.read.projection.domain.model.Phase;

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
            int myScore,
            List<GamePlayer> players,
            List<GameActiveEvent> activeEvents) {}
}
