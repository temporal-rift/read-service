package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.DetectedParadox;
import io.github.temporalrift.read.projection.domain.model.ParadoxType;
import io.github.temporalrift.read.projection.domain.port.out.DetectedParadoxRepository;

public abstract class DetectedParadoxContract extends PortContract {

    protected abstract DetectedParadoxRepository port();

    @Test
    void savedParadoxesAreFoundByTheirIds() {
        var first = paradox(gameId, 1);
        var second = paradox(gameId, 1);
        inTransaction(() -> {
            port().saveIfAbsent(first);
            port().saveIfAbsent(second);
            port().saveIfAbsent(paradox(gameId, 1));
        });

        assertThat(inTransaction(
                        () -> port().findByGameIdAndParadoxIds(gameId, List.of(first.paradoxId(), second.paradoxId()))))
                .containsExactlyInAnyOrder(first, second);
        assertThat(inTransaction(() -> port().findByGameIdAndParadoxIds(otherGameId, List.of(first.paradoxId()))))
                .isEmpty();
    }

    @Test
    void lookingUpNoIdsFindsNothing() {
        inTransaction(() -> port().saveIfAbsent(paradox(gameId, 1)));

        assertThat(inTransaction(() -> port().findByGameIdAndParadoxIds(gameId, List.of())))
                .isEmpty();
    }

    @Test
    void aRecordedParadoxNeverChanges() {
        var first = paradox(gameId, 1);
        inTransaction(() -> port().saveIfAbsent(first));

        inTransaction(() -> port().saveIfAbsent(new DetectedParadox(
                gameId, 2, first.paradoxId(), ParadoxType.CHAIN_CONFLICT, UUID.randomUUID(), List.of())));

        assertThat(inTransaction(() -> port().findByGameIdAndParadoxIds(gameId, List.of(first.paradoxId()))))
                .containsExactly(first);
    }

    @Test
    void deletesRemoveOnlyTheRequestedScope() {
        var era1 = paradox(gameId, 1);
        var era2 = paradox(gameId, 2);
        var elsewhere = paradox(otherGameId, 1);
        inTransaction(() -> {
            port().saveIfAbsent(era1);
            port().saveIfAbsent(era2);
            port().saveIfAbsent(elsewhere);
        });
        var ids = List.of(era1.paradoxId(), era2.paradoxId());

        inTransaction(() -> port().deleteByGameIdAndEraNumber(gameId, 1));
        assertThat(inTransaction(() -> port().findByGameIdAndParadoxIds(gameId, ids)))
                .containsExactly(era2);

        inTransaction(() -> port().deleteByGameId(gameId));
        assertThat(inTransaction(() -> port().findByGameIdAndParadoxIds(gameId, ids)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndParadoxIds(otherGameId, List.of(elsewhere.paradoxId()))))
                .containsExactly(elsewhere);
    }

    private static DetectedParadox paradox(UUID gameId, int eraNumber) {
        return new DetectedParadox(
                gameId,
                eraNumber,
                UUID.randomUUID(),
                ParadoxType.DEAD_HEAT,
                UUID.randomUUID(),
                List.of(UUID.randomUUID(), UUID.randomUUID()));
    }
}
