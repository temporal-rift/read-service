package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import io.github.temporalrift.read.projection.domain.model.EventOutcome;
import io.github.temporalrift.read.projection.domain.model.GameHistoryProjection;
import io.github.temporalrift.read.projection.domain.model.HistoryEventDefinition;
import io.github.temporalrift.read.projection.domain.port.out.GameHistoryRepository;

public abstract class GameHistoryContract extends PortContract {

    protected abstract GameHistoryRepository port();

    @Test
    void anEraSnapshotIsReadBackByGameAndEra() {
        var history = history(gameId, 1);

        inTransaction(() -> port().save(history));

        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1)))
                .contains(history);
        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 2)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(otherGameId, 1)))
                .isEmpty();
    }

    @Test
    void erasAreListedInAscendingOrder() {
        inTransaction(() -> {
            port().save(history(gameId, 3));
            port().save(history(gameId, 1));
            port().save(history(gameId, 2));
        });

        assertThat(inTransaction(() -> port().findByGameId(gameId)))
                .extracting(GameHistoryProjection::eraNumber)
                .containsExactly(1, 2, 3);
    }

    @Test
    void savingAgainUpdatesTheSnapshot() {
        var eventId = UUID.randomUUID();
        inTransaction(() -> port().save(history(gameId, 1)));

        var updated = inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1))
                .orElseThrow()
                .recordCascade(eventId)
                .close(1);
        inTransaction(() -> port().save(updated));

        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1)))
                .contains(updated);
    }

    @Test
    void aWriteBasedOnAStaleReadConflictsInsteadOfOverwriting() {
        inTransaction(() -> port().save(history(gameId, 1)));

        assertThatThrownBy(() -> inTransaction(() -> {
                    var seen = port().findByGameIdAndEraNumber(gameId, 1).orElseThrow();
                    CompletableFuture.runAsync(() -> inTransaction(() -> {
                                var concurrent = port().findByGameIdAndEraNumber(gameId, 1)
                                        .orElseThrow();
                                port().save(concurrent.recordCascade(UUID.randomUUID()));
                            }))
                            .join();
                    port().save(seen.recordCascade(UUID.randomUUID()));
                }))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1))
                        .orElseThrow()
                        .cascadedEventReferences())
                .hasSize(1);
    }

    private static GameHistoryProjection history(UUID gameId, int eraNumber) {
        return GameHistoryProjection.empty(gameId, eraNumber)
                .mergeEventDefinitions(List.of(new HistoryEventDefinition(
                        UUID.randomUUID(),
                        0,
                        "Event " + eraNumber,
                        List.of(new EventOutcome(UUID.randomUUID(), "Outcome " + eraNumber, 34)))));
    }
}
