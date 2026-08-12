package io.github.temporalrift.read.projection.application.port.in;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.CascadedEvent;
import io.github.temporalrift.read.projection.domain.model.DealtCard;
import io.github.temporalrift.read.projection.domain.model.ResolvedOutcome;

/** Retrieves the shared history for one game, scoping dealt-hand data to the requesting player. */
public interface GetGameHistoryUseCase {

    /**
     * Loads all available eras in ascending order.
     *
     * @throws io.github.temporalrift.read.projection.domain.model.GameHistoryNotFoundException when the game has no
     *     history rows
     */
    Result get(UUID gameId, UUID playerId);

    record Result(UUID gameId, List<EraResult> eras) {
        public Result {
            eras = List.copyOf(eras);
        }
    }

    record EraResult(
            int eraNumber,
            List<ResolvedOutcome> outcomes,
            int paradoxesCascaded,
            List<CascadedEvent> cascadedEvents,
            List<DealtCard> myHand) {
        public EraResult {
            outcomes = List.copyOf(outcomes);
            cascadedEvents = List.copyOf(cascadedEvents);
            myHand = List.copyOf(myHand);
        }
    }
}
