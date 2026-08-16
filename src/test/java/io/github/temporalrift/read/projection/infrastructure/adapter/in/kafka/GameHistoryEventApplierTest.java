package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardGrade;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CardType;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EraEndedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventsDrawnFutureEvent;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventsDrawnOutcome;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventsDrawnPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandDealtCardInstance;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandSelectedPayload;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.HandSelectionOrigin;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.OutcomeAppliedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxCascadedPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.ParadoxCascadedProbabilityState;
import io.github.temporalrift.read.projection.domain.model.CarryForwardProbability;
import io.github.temporalrift.read.projection.domain.model.GameHistoryProjection;
import io.github.temporalrift.read.projection.domain.port.out.GameHistoryRepository;

class GameHistoryEventApplierTest {

    private final InMemoryHistoryRepository histories = new InMemoryHistoryRepository();
    private final GameHistoryEventApplier applier = new GameHistoryEventApplier(histories);
    private final UUID gameId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID outcomeId = UUID.randomUUID();

    @Test
    void eventsDrawnAndOutcomeApplied_materializeResolvedOutcome() {
        applier.applyEventsDrawn(eventsDrawn());
        applier.applyOutcomeApplied(outcomeApplied());

        assertThat(history().resolvedOutcomes().getFirst().winningOutcomeDescription())
                .isEqualTo("Outcome");
    }

    @Test
    void terminalBeforeMetadata_correlatesWhenEventsDrawnArrives() {
        applier.applyOutcomeApplied(outcomeApplied());
        assertThat(history().resolvedOutcomes()).isEmpty();

        applier.applyEventsDrawn(eventsDrawn());

        assertThat(history().resolvedOutcomes()).hasSize(1);
    }

    @Test
    void paradoxCascaded_duplicateDelivery_isSetLike() {
        var payload = paradoxCascaded();

        applier.applyParadoxCascaded(payload);
        applier.applyParadoxCascaded(payload);
        applier.applyEventsDrawn(eventsDrawn());

        assertThat(history().cascadedEvents()).hasSize(1);
        assertThat(history().paradoxesCascaded()).isEqualTo(1);
        assertThat(history().cascadedEventReferences().getFirst().carryForwardProbabilityState())
                .containsExactly(new CarryForwardProbability(outcomeId, 45));
    }

    @Test
    void eraEnded_recordsAuthoritativeCascadeCountWithoutBlockingLaterCorrelation() {
        applier.applyEraEnded(new EraEndedPayload(gameId, 1, 2, 2));
        applier.applyParadoxCascaded(paradoxCascaded());
        applier.applyEventsDrawn(eventsDrawn());

        assertThat(history().closed()).isTrue();
        assertThat(history().paradoxesCascaded()).isEqualTo(2);
        assertThat(history().cascadedEvents()).hasSize(1);
    }

    @Test
    void handSelected_recordsDealtCardsForPlayer() {
        var playerId = UUID.randomUUID();
        var cardInstanceId = UUID.randomUUID();

        applier.applyHandSelected(new HandSelectedPayload(
                gameId,
                1,
                playerId,
                HandSelectionOrigin.PLAYER,
                List.of(new HandDealtCardInstance(cardInstanceId, CardType.PUSH, CardGrade.II, 1))));

        assertThat(history().myHand(playerId))
                .containsExactly(new io.github.temporalrift.read.projection.domain.model.DealtCard(
                        cardInstanceId, "PUSH", "II", 1));
    }

    @Test
    void handSelected_duplicateDelivery_isIdempotent() {
        var playerId = UUID.randomUUID();
        var payload = new HandSelectedPayload(
                gameId,
                1,
                playerId,
                HandSelectionOrigin.TIMEOUT_RANDOM,
                List.of(new HandDealtCardInstance(UUID.randomUUID(), CardType.PUSH, CardGrade.I, 1)));

        applier.applyHandSelected(payload);
        applier.applyHandSelected(payload);

        assertThat(history().myHand(playerId)).hasSize(1);
    }

    private EventsDrawnPayload eventsDrawn() {
        return new EventsDrawnPayload(
                gameId,
                1,
                List.of(new EventsDrawnFutureEvent(
                        eventId,
                        "Event",
                        List.of(new EventsDrawnOutcome(outcomeId, "Outcome", 100)),
                        io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CarryOverState.FRESH)));
    }

    private OutcomeAppliedPayload outcomeApplied() {
        return new OutcomeAppliedPayload(gameId, 1, eventId, outcomeId, List.of());
    }

    private ParadoxCascadedPayload paradoxCascaded() {
        return new ParadoxCascadedPayload(
                gameId,
                1,
                UUID.randomUUID(),
                eventId,
                List.of(new ParadoxCascadedProbabilityState(outcomeId, 45)),
                null);
    }

    private GameHistoryProjection history() {
        return histories.findByGameIdAndEraNumber(gameId, 1).orElseThrow();
    }

    private static final class InMemoryHistoryRepository implements GameHistoryRepository {

        private final Map<Key, GameHistoryProjection> values = new HashMap<>();

        @Override
        public Optional<GameHistoryProjection> findByGameIdAndEraNumber(UUID gameId, int eraNumber) {
            return Optional.ofNullable(values.get(new Key(gameId, eraNumber)));
        }

        @Override
        public List<GameHistoryProjection> findByGameId(UUID gameId) {
            var result = new ArrayList<>(values.values().stream()
                    .filter(value -> value.gameId().equals(gameId))
                    .toList());
            result.sort(Comparator.comparingInt(GameHistoryProjection::eraNumber));
            return List.copyOf(result);
        }

        @Override
        public void save(GameHistoryProjection projection) {
            values.put(new Key(projection.gameId(), projection.eraNumber()), projection);
        }

        private record Key(UUID gameId, int eraNumber) {}
    }
}
