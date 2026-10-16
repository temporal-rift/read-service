package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.ExposeFact;
import io.github.temporalrift.read.projection.domain.port.out.ExposeFactRepository;

public abstract class ExposeFactContract extends PortContract {

    protected abstract ExposeFactRepository port();

    @Test
    void factsOfAnEraAreOrderedByActivistTargetAndRound() {
        var lowActivistRound2 = fact(gameId, 1, LOW, LOW, 2, false);
        var lowActivistRound1 = fact(gameId, 1, LOW, LOW, 1, false);
        var highActivist = fact(gameId, 1, HIGH, LOW, 1, true);
        var lowActivistHighTarget = fact(gameId, 1, LOW, HIGH, 1, false);
        inTransaction(() -> {
            port().upsert(highActivist);
            port().upsert(lowActivistRound2);
            port().upsert(lowActivistHighTarget);
            port().upsert(lowActivistRound1);
        });

        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1)))
                .containsExactly(lowActivistRound1, lowActivistRound2, lowActivistHighTarget, highActivist);
    }

    @Test
    void upsertReplacesTheFactWithTheSameActivistTargetAndRound() {
        inTransaction(() -> port().upsert(fact(gameId, 1, LOW, HIGH, 1, false)));

        var replacement = fact(gameId, 1, LOW, HIGH, 1, true);
        inTransaction(() -> port().upsert(replacement));

        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1)))
                .containsExactly(replacement);
    }

    @Test
    void deletesRemoveOnlyTheRequestedScope() {
        var era1 = fact(gameId, 1, LOW, HIGH, 1, false);
        var era2 = fact(gameId, 2, LOW, HIGH, 1, false);
        var elsewhere = fact(otherGameId, 1, LOW, HIGH, 1, false);
        inTransaction(() -> {
            port().upsert(era1);
            port().upsert(era2);
            port().upsert(elsewhere);
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

    private static ExposeFact fact(
            UUID gameId, int eraNumber, UUID activist, UUID target, int roundNumber, boolean behaviorChanged) {
        return new ExposeFact(
                gameId,
                eraNumber,
                activist,
                target,
                roundNumber,
                "SIGNATURE",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                behaviorChanged);
    }
}
