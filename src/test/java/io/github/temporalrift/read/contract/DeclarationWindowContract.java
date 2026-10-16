package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.DeclarationWindow;
import io.github.temporalrift.read.projection.domain.port.out.DeclarationWindowRepository;

public abstract class DeclarationWindowContract extends PortContract {

    private static final Instant EXPIRES_AT = Instant.parse("2026-08-15T00:00:00Z");

    protected abstract DeclarationWindowRepository port();

    @Test
    void aSavedWindowIsFoundByGameAndEra() {
        var window = new DeclarationWindow(gameId, 2, EXPIRES_AT);

        inTransaction(() -> port().saveIfAbsent(window));

        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 2)))
                .contains(window);
        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1)))
                .isEmpty();
    }

    @Test
    void theFirstRecordedWindowIsKept() {
        var first = new DeclarationWindow(gameId, 2, EXPIRES_AT);
        inTransaction(() -> port().saveIfAbsent(first));

        inTransaction(() -> port().saveIfAbsent(new DeclarationWindow(gameId, 2, EXPIRES_AT.plusSeconds(60))));

        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 2)))
                .contains(first);
    }

    @Test
    void deletesRemoveOnlyTheRequestedScope() {
        var era1 = new DeclarationWindow(gameId, 1, EXPIRES_AT);
        var era2 = new DeclarationWindow(gameId, 2, EXPIRES_AT);
        var elsewhere = new DeclarationWindow(otherGameId, 1, EXPIRES_AT);
        inTransaction(() -> {
            port().saveIfAbsent(era1);
            port().saveIfAbsent(era2);
            port().saveIfAbsent(elsewhere);
        });

        inTransaction(() -> port().deleteByGameIdAndEraNumber(gameId, 1));
        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 1)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 2)))
                .contains(era2);

        inTransaction(() -> port().deleteByGameId(gameId));
        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(gameId, 2)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndEraNumber(otherGameId, 1)))
                .contains(elsewhere);
    }
}
