package io.github.temporalrift.read.projection.application.port.in;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.CascadedEvent;
import io.github.temporalrift.read.projection.domain.model.ResolvedOutcome;

/** Retrieves the bounded shared history for one game without applying player-specific filtering. */
public interface GetGameHistoryUseCase {

    /**
     * Loads all available eras in ascending order.
     *
     * @throws io.github.temporalrift.read.projection.domain.model.GameHistoryNotFoundException when the game has no
     *     history rows
     */
    Result get(UUID gameId);

    record Result(UUID gameId, List<EraResult> eras) {
        public Result {
            eras = List.copyOf(eras);
        }
    }

    record EraResult(
            int eraNumber, List<ResolvedOutcome> outcomes, int paradoxesCascaded, List<CascadedEvent> cascadedEvents) {
        public EraResult {
            outcomes = List.copyOf(outcomes);
            cascadedEvents = List.copyOf(cascadedEvents);
        }
    }
}
