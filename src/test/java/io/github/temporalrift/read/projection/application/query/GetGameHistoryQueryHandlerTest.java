package io.github.temporalrift.read.projection.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.read.projection.domain.model.EventOutcome;
import io.github.temporalrift.read.projection.domain.model.GameHistoryNotFoundException;
import io.github.temporalrift.read.projection.domain.model.GameHistoryProjection;
import io.github.temporalrift.read.projection.domain.model.HistoryEventDefinition;
import io.github.temporalrift.read.projection.domain.port.out.GameHistoryRepository;

@ExtendWith(MockitoExtension.class)
class GetGameHistoryQueryHandlerTest {

    @Mock
    GameHistoryRepository histories;

    @InjectMocks
    GetGameHistoryQueryHandler handler;

    private final UUID gameId = UUID.randomUUID();

    @Test
    void get_singleEra_materializesResolvedOutcome() {
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        given(histories.findByGameId(gameId))
                .willReturn(List.of(history(1, eventId, outcomeId).recordResolvedOutcome(eventId, outcomeId)));

        var result = handler.get(gameId);

        assertThat(result.eras()).hasSize(1);
        assertThat(result.eras().getFirst().outcomes().getFirst().winningOutcomeDescription())
                .isEqualTo("Outcome 1");
    }

    @Test
    void get_cascade_materializesEventAndCount() {
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        given(histories.findByGameId(gameId))
                .willReturn(List.of(history(1, eventId, outcomeId)
                        .recordCascade(eventId, List.of())
                        .close(1)));

        var era = handler.get(gameId).eras().getFirst();

        assertThat(era.paradoxesCascaded()).isEqualTo(1);
        assertThat(era.cascadedEvents()).extracting(event -> event.eventId()).containsExactly(eventId);
    }

    @Test
    void get_multiEra_sortsAscending() {
        var firstEvent = UUID.randomUUID();
        var secondEvent = UUID.randomUUID();
        given(histories.findByGameId(gameId))
                .willReturn(
                        List.of(history(2, secondEvent, UUID.randomUUID()), history(1, firstEvent, UUID.randomUUID())));

        assertThat(handler.get(gameId).eras())
                .extracting(era -> era.eraNumber())
                .containsExactly(1, 2);
    }

    @Test
    void get_partialCorrelation_omitsIncompleteChildren() {
        var incomplete = GameHistoryProjection.empty(gameId, 1)
                .recordResolvedOutcome(UUID.randomUUID(), UUID.randomUUID())
                .recordCascade(UUID.randomUUID(), List.of());
        given(histories.findByGameId(gameId)).willReturn(List.of(incomplete));

        var era = handler.get(gameId).eras().getFirst();

        assertThat(era.outcomes()).isEmpty();
        assertThat(era.cascadedEvents()).isEmpty();
    }

    @Test
    void get_noRows_throwsHistoryNotFound() {
        given(histories.findByGameId(gameId)).willReturn(List.of());

        assertThatThrownBy(() -> handler.get(gameId))
                .isInstanceOf(GameHistoryNotFoundException.class)
                .hasMessageContaining(gameId.toString());
    }

    private GameHistoryProjection history(int eraNumber, UUID eventId, UUID outcomeId) {
        return GameHistoryProjection.empty(gameId, eraNumber)
                .mergeEventDefinitions(List.of(new HistoryEventDefinition(
                        eventId,
                        0,
                        "Event " + eraNumber,
                        List.of(new EventOutcome(outcomeId, "Outcome " + eraNumber)))));
    }
}
