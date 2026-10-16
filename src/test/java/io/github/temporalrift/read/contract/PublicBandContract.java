package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.PublicBand;
import io.github.temporalrift.read.projection.domain.port.out.PublicBandRepository;

public abstract class PublicBandContract extends PortContract {

    protected abstract PublicBandRepository port();

    @Test
    void bandsOfAnEraAreOrderedByEvent() {
        var high = band(gameId, 1, HIGH, 1, "HIGH");
        var low = band(gameId, 1, LOW, 1, "LOW");

        inTransaction(() -> port().replaceAll(gameId, 1, List.of(high, low)));

        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1)))
                .containsExactly(low, high);
    }

    @Test
    void replaceAllUpdatesKeptEventsRemovesDroppedOnesAndAddsNewOnes() {
        var kept = band(gameId, 1, LOW, 1, "LOW");
        var dropped = band(gameId, 1, UUID.randomUUID(), 1, "MEDIUM");
        inTransaction(() -> port().replaceAll(gameId, 1, List.of(kept, dropped)));

        var updated = band(gameId, 1, LOW, 2, "HIGH");
        var added = band(gameId, 1, HIGH, 2, "LOW");
        inTransaction(() -> port().replaceAll(gameId, 1, List.of(updated, added)));

        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1)))
                .containsExactly(updated, added);
    }

    @Test
    void replacingOneEraLeavesTheOthersUntouched() {
        var era2 = band(gameId, 2, LOW, 1, "LOW");
        inTransaction(() -> {
            port().replaceAll(gameId, 2, List.of(era2));
            port().replaceAll(gameId, 1, List.of(band(gameId, 1, LOW, 1, "LOW")));
        });

        inTransaction(() -> port().replaceAll(gameId, 1, List.of()));

        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 2)))
                .containsExactly(era2);
    }

    @Test
    void deletesRemoveOnlyTheRequestedScope() {
        var era1 = band(gameId, 1, LOW, 1, "LOW");
        var era2 = band(gameId, 2, LOW, 1, "LOW");
        var elsewhere = band(otherGameId, 1, LOW, 1, "LOW");
        inTransaction(() -> {
            port().replaceAll(gameId, 1, List.of(era1));
            port().replaceAll(gameId, 2, List.of(era2));
            port().replaceAll(otherGameId, 1, List.of(elsewhere));
        });

        inTransaction(() -> port().deleteByGameIdAndEraNumber(gameId, 1));
        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 2)))
                .containsExactly(era2);

        inTransaction(() -> port().deleteByGameId(gameId));
        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 2)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(otherGameId, 1)))
                .containsExactly(elsewhere);
    }

    private static PublicBand band(UUID gameId, int eraNumber, UUID eventId, int observedInRound, String band) {
        return new PublicBand(
                gameId,
                eraNumber,
                eventId,
                observedInRound,
                List.of(new PublicBand.OutcomeBand(UUID.randomUUID(), band)));
    }
}
