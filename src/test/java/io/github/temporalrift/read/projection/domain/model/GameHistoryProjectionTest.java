package io.github.temporalrift.read.projection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class GameHistoryProjectionTest {

    private final UUID gameId = UUID.randomUUID();
    private final UUID firstEventId = UUID.randomUUID();
    private final UUID secondEventId = UUID.randomUUID();
    private final UUID firstOutcomeId = UUID.randomUUID();
    private final UUID secondOutcomeId = UUID.randomUUID();
    private final CarryForwardProbability carryForwardProbability = new CarryForwardProbability(secondOutcomeId, 45);

    @Test
    void terminalFactsBeforeMetadata_materializeAfterDefinitionsArriveInRevealOrder() {
        var projection = GameHistoryProjection.empty(gameId, 1)
                .recordResolvedOutcome(secondEventId, secondOutcomeId)
                .recordResolvedOutcome(firstEventId, firstOutcomeId)
                .recordCascade(secondEventId, List.of(carryForwardProbability))
                .mergeEventDefinitions(definitions());

        assertThat(projection.resolvedOutcomes())
                .extracting(ResolvedOutcome::eventId)
                .containsExactly(firstEventId, secondEventId);
        assertThat(projection.cascadedEvents()).containsExactly(new CascadedEvent(secondEventId, "Second event"));
        assertThat(projection.cascadedEventReferences().getFirst().carryForwardProbabilityState())
                .containsExactly(carryForwardProbability);
    }

    @Test
    void duplicateTerminalFacts_areIdempotent() {
        var projection = GameHistoryProjection.empty(gameId, 1)
                .recordResolvedOutcome(firstEventId, firstOutcomeId)
                .recordResolvedOutcome(firstEventId, firstOutcomeId)
                .recordCascade(secondEventId, List.of(carryForwardProbability))
                .recordCascade(secondEventId, List.of(carryForwardProbability))
                .mergeEventDefinitions(definitions());

        assertThat(projection.resolvedOutcomes()).hasSize(1);
        assertThat(projection.cascadedEvents()).hasSize(1);
        assertThat(projection.paradoxesCascaded()).isEqualTo(1);
    }

    @Test
    void eraClosure_replacesObservedCascadeCountWithAuthoritativeValue() {
        var projection = GameHistoryProjection.empty(gameId, 1)
                .recordCascade(firstEventId, List.of())
                .close(2);

        assertThat(projection.closed()).isTrue();
        assertThat(projection.paradoxesCascaded()).isEqualTo(2);
    }

    @Test
    void incompleteCorrelation_omitsInvalidResponseItems() {
        var projection = GameHistoryProjection.empty(gameId, 1)
                .recordResolvedOutcome(firstEventId, firstOutcomeId)
                .recordCascade(secondEventId, List.of(carryForwardProbability));

        assertThat(projection.resolvedOutcomes()).isEmpty();
        assertThat(projection.cascadedEvents()).isEmpty();
    }

    @Test
    void dealtHand_isRecordedForPlayer() {
        var playerId = UUID.randomUUID();
        var card = new DealtCard(UUID.randomUUID(), "PUSH", "I");

        var projection = GameHistoryProjection.empty(gameId, 1).recordDealtHand(playerId, List.of(card));

        assertThat(projection.myHand(playerId)).containsExactly(card);
    }

    @Test
    void dealtHand_duplicateDelivery_isIdempotent() {
        var playerId = UUID.randomUUID();
        var card = new DealtCard(UUID.randomUUID(), "PUSH", "I");
        var otherCard = new DealtCard(UUID.randomUUID(), "SCAN", "II");

        var projection = GameHistoryProjection.empty(gameId, 1)
                .recordDealtHand(playerId, List.of(card))
                .recordDealtHand(playerId, List.of(otherCard));

        assertThat(projection.myHand(playerId)).containsExactly(card);
    }

    @Test
    void dealtHand_forDifferentPlayers_remainsIndependent() {
        var firstPlayerId = UUID.randomUUID();
        var secondPlayerId = UUID.randomUUID();
        var firstCard = new DealtCard(UUID.randomUUID(), "PUSH", "I");
        var secondCard = new DealtCard(UUID.randomUUID(), "SCAN", "II");

        var projection = GameHistoryProjection.empty(gameId, 1)
                .recordDealtHand(firstPlayerId, List.of(firstCard))
                .recordDealtHand(secondPlayerId, List.of(secondCard));

        assertThat(projection.myHand(firstPlayerId)).containsExactly(firstCard);
        assertThat(projection.myHand(secondPlayerId)).containsExactly(secondCard);
    }

    @Test
    void dealtHand_forPlayerWithNoRecordedHand_isEmpty() {
        var projection = GameHistoryProjection.empty(gameId, 1);

        assertThat(projection.myHand(UUID.randomUUID())).isEmpty();
    }

    @Test
    void eventDefinition_rejectsDescriptionThatWouldViolateTheRestContract() {
        assertThatThrownBy(() -> new HistoryEventDefinition(
                        firstEventId, 0, "Event", List.of(new EventOutcome(firstOutcomeId, " "))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonblank descriptions");
    }

    private List<HistoryEventDefinition> definitions() {
        return List.of(
                new HistoryEventDefinition(
                        firstEventId, 0, "First event", List.of(new EventOutcome(firstOutcomeId, "First outcome"))),
                new HistoryEventDefinition(
                        secondEventId,
                        1,
                        "Second event",
                        List.of(new EventOutcome(secondOutcomeId, "Second outcome"))));
    }
}
