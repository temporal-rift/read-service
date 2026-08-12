package io.github.temporalrift.read.projection.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Order-independent correlation state for one game era.
 *
 * <p>Terminal facts and event definitions are merged by stable IDs so topic replay, duplicate delivery,
 * and cross-topic arrival order produce the same materialized history.
 */
public record GameHistoryProjection(
        UUID gameId,
        int eraNumber,
        List<HistoryEventDefinition> eventDefinitions,
        List<ResolvedOutcomeReference> resolvedOutcomeReferences,
        List<CascadedEventReference> cascadedEventReferences,
        List<DealtHand> dealtHands,
        int cascadedParadoxCount,
        boolean closed) {

    public GameHistoryProjection {
        gameId = Objects.requireNonNull(gameId, "gameId must not be null");
        if (eraNumber < 1) {
            throw new IllegalArgumentException("eraNumber must be at least 1");
        }
        eventDefinitions = List.copyOf(eventDefinitions);
        resolvedOutcomeReferences = List.copyOf(resolvedOutcomeReferences);
        cascadedEventReferences = List.copyOf(cascadedEventReferences);
        dealtHands = List.copyOf(dealtHands);
        if (cascadedParadoxCount < 0) {
            throw new IllegalArgumentException("cascadedParadoxCount must not be negative");
        }
    }

    public static GameHistoryProjection empty(UUID gameId, int eraNumber) {
        return new GameHistoryProjection(gameId, eraNumber, List.of(), List.of(), List.of(), List.of(), 0, false);
    }

    public GameHistoryProjection mergeEventDefinitions(List<HistoryEventDefinition> definitions) {
        var merged = new LinkedHashMap<UUID, HistoryEventDefinition>();
        eventDefinitions.forEach(definition -> merged.put(definition.eventId(), definition));
        definitions.forEach(definition -> merged.putIfAbsent(definition.eventId(), definition));
        return copy(
                List.copyOf(merged.values()),
                resolvedOutcomeReferences,
                cascadedEventReferences,
                dealtHands,
                cascadedParadoxCount,
                closed);
    }

    public GameHistoryProjection recordResolvedOutcome(UUID eventId, UUID winningOutcomeId) {
        if (resolvedOutcomeReferences.stream()
                .anyMatch(reference -> reference.eventId().equals(eventId))) {
            return this;
        }
        var references = new ArrayList<>(resolvedOutcomeReferences);
        references.add(new ResolvedOutcomeReference(eventId, winningOutcomeId));
        return copy(
                eventDefinitions,
                List.copyOf(references),
                cascadedEventReferences,
                dealtHands,
                cascadedParadoxCount,
                closed);
    }

    public GameHistoryProjection recordCascade(
            UUID eventId, List<CarryForwardProbability> carryForwardProbabilityState) {
        if (cascadedEventReferences.stream()
                .anyMatch(reference -> reference.eventId().equals(eventId))) {
            return this;
        }
        var references = new ArrayList<>(cascadedEventReferences);
        references.add(new CascadedEventReference(eventId, carryForwardProbabilityState));
        return copy(
                eventDefinitions,
                resolvedOutcomeReferences,
                List.copyOf(references),
                dealtHands,
                cascadedParadoxCount,
                closed);
    }

    // First-write-wins by design, unlike the live projection's replace-on-HandDealt semantics: this is the
    // durable record of what was originally dealt, so a same-era re-deal (if one were ever emitted) must not
    // overwrite it.
    public GameHistoryProjection recordDealtHand(UUID playerId, List<DealtCard> cards) {
        if (dealtHands.stream().anyMatch(hand -> hand.playerId().equals(playerId))) {
            return this;
        }
        var hands = new ArrayList<>(dealtHands);
        hands.add(new DealtHand(playerId, cards));
        return copy(
                eventDefinitions,
                resolvedOutcomeReferences,
                cascadedEventReferences,
                List.copyOf(hands),
                cascadedParadoxCount,
                closed);
    }

    public GameHistoryProjection close(int authoritativeCascadeCount) {
        if (authoritativeCascadeCount < 0) {
            throw new IllegalArgumentException("authoritativeCascadeCount must not be negative");
        }
        return copy(
                eventDefinitions,
                resolvedOutcomeReferences,
                cascadedEventReferences,
                dealtHands,
                authoritativeCascadeCount,
                true);
    }

    public List<DealtCard> myHand(UUID playerId) {
        return dealtHands.stream()
                .filter(hand -> hand.playerId().equals(playerId))
                .findFirst()
                .map(DealtHand::cards)
                .orElse(List.of());
    }

    public int paradoxesCascaded() {
        return closed ? cascadedParadoxCount : cascadedEventReferences.size();
    }

    public List<ResolvedOutcome> resolvedOutcomes() {
        var definitions = definitionsById();
        return resolvedOutcomeReferences.stream()
                .map(reference -> materialize(reference, definitions.get(reference.eventId())))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(
                        outcome -> definitions.get(outcome.eventId()).revealIndex()))
                .toList();
    }

    public List<CascadedEvent> cascadedEvents() {
        var definitions = definitionsById();
        return cascadedEventReferences.stream()
                .map(reference -> definitions.containsKey(reference.eventId())
                        ? new CascadedEvent(
                                reference.eventId(),
                                definitions.get(reference.eventId()).title())
                        : null)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(
                        event -> definitions.get(event.eventId()).revealIndex()))
                .toList();
    }

    private ResolvedOutcome materialize(ResolvedOutcomeReference reference, HistoryEventDefinition definition) {
        if (definition == null) {
            return null;
        }
        return definition.outcomes().stream()
                .filter(outcome -> outcome.outcomeId().equals(reference.winningOutcomeId()))
                .findFirst()
                .map(outcome -> new ResolvedOutcome(
                        reference.eventId(), definition.title(), reference.winningOutcomeId(), outcome.description()))
                .orElse(null);
    }

    private java.util.Map<UUID, HistoryEventDefinition> definitionsById() {
        return eventDefinitions.stream()
                .collect(Collectors.toUnmodifiableMap(HistoryEventDefinition::eventId, Function.identity()));
    }

    private GameHistoryProjection copy(
            List<HistoryEventDefinition> definitions,
            List<ResolvedOutcomeReference> outcomes,
            List<CascadedEventReference> cascades,
            List<DealtHand> hands,
            int cascadeCount,
            boolean isClosed) {
        return new GameHistoryProjection(
                gameId, eraNumber, definitions, outcomes, cascades, hands, cascadeCount, isClosed);
    }
}
