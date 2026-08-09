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

import io.github.temporalrift.read.projection.domain.model.CarryForwardProbability;
import io.github.temporalrift.read.projection.domain.model.CarryOverState;
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

    private EventsDrawnPayload eventsDrawn() {
        return new EventsDrawnPayload(
                gameId,
                1,
                List.of(new EventsDrawnPayload.DrawnEvent(
                        eventId,
                        "Event",
                        List.of(new EventsDrawnPayload.DrawnOutcome(outcomeId, "Outcome", 100)),
                        CarryOverState.FRESH)));
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
                List.of(new ParadoxCascadedPayload.CarryForwardProbability(outcomeId, 45)));
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
