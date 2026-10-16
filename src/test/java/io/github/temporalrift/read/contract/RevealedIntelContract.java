package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.RevealedIntelEntry;
import io.github.temporalrift.read.projection.domain.port.out.RevealedIntelRepository;

/** Behaviour shared by the three per-kind revealed-intel ports. */
public abstract class RevealedIntelContract<D extends RevealedIntelEntry> extends PortContract {

    private final UUID viewer = UUID.randomUUID();

    protected abstract RevealedIntelRepository<D> port();

    /** Builds an entry of the port's kind; entries with different {@code marker}s differ in content. */
    protected abstract D intel(UUID gameId, UUID viewer, int eraNumber, UUID eventId, int observedInRound, int marker);

    @Test
    void entriesOfAViewerAndEraAreOrderedByEvent() {
        var high = intel(gameId, viewer, 1, HIGH, 1, 1);
        var low = intel(gameId, viewer, 1, LOW, 1, 2);
        inTransaction(() -> {
            port().upsertLatest(high);
            port().upsertLatest(low);
        });

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, viewer, 1)))
                .containsExactly(low, high);
    }

    @Test
    void entriesAreScopedToTheirViewerEraAndGame() {
        inTransaction(() -> port().upsertLatest(intel(gameId, viewer, 1, LOW, 1, 1)));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, UUID.randomUUID(), 1)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, viewer, 2)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(otherGameId, viewer, 1)))
                .isEmpty();
    }

    @Test
    void aLaterOrEqualRoundReplacesTheEntry() {
        inTransaction(() -> port().upsertLatest(intel(gameId, viewer, 1, LOW, 2, 1)));

        var sameRound = intel(gameId, viewer, 1, LOW, 2, 2);
        inTransaction(() -> port().upsertLatest(sameRound));
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, viewer, 1)))
                .containsExactly(sameRound);

        var laterRound = intel(gameId, viewer, 1, LOW, 3, 3);
        inTransaction(() -> port().upsertLatest(laterRound));
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, viewer, 1)))
                .containsExactly(laterRound);
    }

    @Test
    void anEarlierRoundNeverReplacesANewerEntry() {
        var newer = intel(gameId, viewer, 1, LOW, 3, 1);
        inTransaction(() -> port().upsertLatest(newer));

        inTransaction(() -> port().upsertLatest(intel(gameId, viewer, 1, LOW, 2, 2)));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, viewer, 1)))
                .containsExactly(newer);
    }

    @Test
    void deletesRemoveOnlyTheRequestedScope() {
        var era1 = intel(gameId, viewer, 1, LOW, 1, 1);
        var era2 = intel(gameId, viewer, 2, LOW, 1, 2);
        var elsewhere = intel(otherGameId, viewer, 1, LOW, 1, 3);
        inTransaction(() -> {
            port().upsertLatest(era1);
            port().upsertLatest(era2);
            port().upsertLatest(elsewhere);
        });

        inTransaction(() -> port().deleteByGameIdAndEraNumber(gameId, 1));
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, viewer, 1)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, viewer, 2)))
                .containsExactly(era2);

        inTransaction(() -> port().deleteByGameId(gameId));
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, viewer, 2)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(otherGameId, viewer, 1)))
                .containsExactly(elsewhere);
    }
}
