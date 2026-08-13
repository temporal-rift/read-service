package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EraEndedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventsDrawnPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandDealtPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.OutcomeAppliedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxCascadedPayload;
import io.github.temporalrift.read.projection.domain.model.CarryForwardProbability;
import io.github.temporalrift.read.projection.domain.model.DealtCard;
import io.github.temporalrift.read.projection.domain.model.EventOutcome;
import io.github.temporalrift.read.projection.domain.model.GameHistoryProjection;
import io.github.temporalrift.read.projection.domain.model.HistoryEventDefinition;
import io.github.temporalrift.read.projection.domain.port.out.GameHistoryRepository;

/** Applies the five replayable source facts to the independent game-history correlation state. */
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
        var carryForwardProbabilityState = payload.carryForwardProbabilityState().stream()
                .map(state -> new CarryForwardProbability(state.outcomeId(), state.probability()))
                .toList();
        update(
                payload.gameId(),
                payload.eraNumber(),
                history -> history.recordCascade(payload.affectedEventId(), carryForwardProbabilityState));
    }

    void applyEraEnded(EraEndedPayload payload) {
        update(payload.gameId(), payload.eraNumber(), history -> history.close(payload.cascadedParadoxCount()));
    }

    void applyHandDealt(HandDealtPayload payload) {
        var cards = payload.cards().stream()
                .map(card ->
                        new DealtCard(card.cardInstanceId(), card.cardType().name()))
                .toList();
        update(payload.gameId(), payload.eraNumber(), history -> history.recordDealtHand(payload.playerId(), cards));
    }

    private void update(UUID gameId, int eraNumber, UnaryOperator<GameHistoryProjection> update) {
        var current = histories
                .findByGameIdAndEraNumber(gameId, eraNumber)
                .orElseGet(() -> GameHistoryProjection.empty(gameId, eraNumber));
        histories.save(update.apply(current));
    }
}
