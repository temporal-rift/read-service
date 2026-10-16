package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.PublicDeclaration;
import io.github.temporalrift.read.projection.domain.port.out.PublicDeclarationRepository;

public abstract class PublicDeclarationContract extends PortContract {

    protected abstract PublicDeclarationRepository port();

    @Test
    void declarationsOfAnEraAreOrderedByPlayer() {
        var high = declaration(gameId, 1, HIGH, "RALLY");
        var low = declaration(gameId, 1, LOW, "MOMENTUM");

        inTransaction(() -> {
            port().upsert(high);
            port().upsert(low);
        });

        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1)))
                .containsExactly(low, high);
    }

    @Test
    void upsertReplacesThePlayersDeclarationOfTheEra() {
        inTransaction(() -> port().upsert(declaration(gameId, 1, LOW, "RALLY")));

        var replacement = declaration(gameId, 1, LOW, "MOMENTUM");
        inTransaction(() -> port().upsert(replacement));

        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1)))
                .containsExactly(replacement);
    }

    @Test
    void deletesRemoveOnlyTheRequestedScope() {
        var era1 = declaration(gameId, 1, LOW, "RALLY");
        var era2 = declaration(gameId, 2, LOW, "RALLY");
        var elsewhere = declaration(otherGameId, 1, LOW, "RALLY");
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

    private static PublicDeclaration declaration(UUID gameId, int eraNumber, UUID playerId, String mode) {
        return new PublicDeclaration(gameId, eraNumber, playerId, mode, UUID.randomUUID(), UUID.randomUUID());
    }
}
