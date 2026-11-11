package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.SpecialAvailability;
import io.github.temporalrift.read.projection.domain.model.SpecialAvailabilityReport;
import io.github.temporalrift.read.projection.domain.port.out.SpecialAvailabilityRepository;

public abstract class SpecialAvailabilityContract extends PortContract {

    private final UUID playerId = UUID.randomUUID();

    protected abstract SpecialAvailabilityRepository port();

    @Test
    void aReportIsReadBackWithItsSpecialsInOrderForItsPlayerOnly() {
        var report = report(playerId, 1, 1);

        inTransaction(() -> port().save(report));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerId(gameId, playerId)))
                .contains(report);
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerId(gameId, UUID.randomUUID())))
                .isEmpty();
    }

    @Test
    void savingReplacesThePlayersSingleReportAcrossRoundsAndEras() {
        inTransaction(() -> port().save(report(playerId, 1, 3)));

        var nextEra = report(playerId, 2, 1);
        inTransaction(() -> port().save(nextEra));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerId(gameId, playerId)))
                .contains(nextEra);
    }

    @Test
    void deleteByGameIdRemovesEveryPlayersReportOfThatGame() {
        var other = UUID.randomUUID();
        inTransaction(() -> {
            port().save(report(playerId, 1, 1));
            port().save(report(other, 1, 1));
        });

        inTransaction(() -> port().deleteByGameId(gameId));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerId(gameId, playerId)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerId(gameId, other)))
                .isEmpty();
    }

    private SpecialAvailabilityReport report(UUID player, int eraNumber, int roundNumber) {
        return new SpecialAvailabilityReport(
                gameId,
                player,
                eraNumber,
                roundNumber,
                List.of(
                        new SpecialAvailability("ANNIHILATE", false, "NOT_ELIGIBLE_THIS_ROUND"),
                        new SpecialAvailability("CORRUPT", true, null),
                        new SpecialAvailability("CASCADE", true, null)));
    }
}
