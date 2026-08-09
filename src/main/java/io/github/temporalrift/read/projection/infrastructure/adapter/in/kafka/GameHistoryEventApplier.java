package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import io.github.temporalrift.read.projection.domain.model.EventOutcome;
import io.github.temporalrift.read.projection.domain.model.GameHistoryProjection;
import io.github.temporalrift.read.projection.domain.model.HistoryEventDefinition;
import io.github.temporalrift.read.projection.domain.port.out.GameHistoryRepository;

/** Applies the four replayable source facts to the independent game-history correlation state. */
@Component
class GameHistoryEventApplier {

    private final GameHistoryRepository histories;

    GameHistoryEventApplier(GameHistoryRepository histories) {
        this.histories = histories;
    }

    void applyEventsDrawn(EventsDrawnPayload payload) {
        var definitions = IntStream.range(0, payload.events().size())
                .mapToObj(index -> {
                    var event = payload.events().get(index);
                    return new HistoryEventDefinition(
                            event.eventId(),
                            index,
                            event.title(),
                            event.outcomes().stream()
                                    .map(outcome -> new EventOutcome(outcome.outcomeId(), outcome.description()))
                                    .toList());
                })
                .toList();
        update(payload.gameId(), payload.eraNumber(), history -> history.mergeEventDefinitions(definitions));
    }

    void applyOutcomeApplied(OutcomeAppliedPayload payload) {
        update(
                payload.gameId(),
                payload.eraNumber(),
                history -> history.recordResolvedOutcome(payload.eventId(), payload.winningOutcomeId()));
    }

    void applyParadoxCascaded(ParadoxCascadedPayload payload) {
        update(payload.gameId(), payload.eraNumber(), history -> history.recordCascade(payload.affectedEventId()));
    }

    void applyEraEnded(EraEndedPayload payload) {
        update(payload.gameId(), payload.eraNumber(), history -> history.close(payload.cascadedParadoxCount()));
    }

    private void update(UUID gameId, int eraNumber, UnaryOperator<GameHistoryProjection> update) {
        var current = histories
                .findByGameIdAndEraNumber(gameId, eraNumber)
                .orElseGet(() -> GameHistoryProjection.empty(gameId, eraNumber));
        histories.save(update.apply(current));
    }
}
