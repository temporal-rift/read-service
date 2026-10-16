package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.ForesightPreview;
import io.github.temporalrift.read.projection.domain.model.ForesightPreviewEvent;
import io.github.temporalrift.read.projection.domain.model.ForesightPreviewOutcome;
import io.github.temporalrift.read.projection.domain.port.out.ForesightPreviewRepository;

public abstract class ForesightPreviewContract extends PortContract {

    private final UUID playerId = UUID.randomUUID();

    protected abstract ForesightPreviewRepository port();

    @Test
    void aPreviewIsReadBackForItsViewerAndEra() {
        var preview = preview(1, "Hidden");

        inTransaction(() -> port().upsert(preview));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 1)))
                .contains(preview);
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, UUID.randomUUID(), 1)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2)))
                .isEmpty();
    }

    @Test
    void upsertReplacesThePreviewOfTheSameViewerAndEra() {
        inTransaction(() -> port().upsert(preview(1, "First")));

        var replacement = preview(1, "Second");
        inTransaction(() -> port().upsert(replacement));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 1)))
                .contains(replacement);
    }

    @Test
    void deletesRemoveOnlyTheRequestedScope() {
        var era1 = preview(1, "One");
        var era2 = preview(2, "Two");
        inTransaction(() -> {
            port().upsert(era1);
            port().upsert(era2);
        });

        inTransaction(() -> port().deleteByGameIdAndEraNumber(gameId, 1));
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 1)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2)))
                .contains(era2);

        inTransaction(() -> port().deleteByGameId(gameId));
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2)))
                .isEmpty();
    }

    private ForesightPreview preview(int eraNumber, String emptyReason) {
        return new ForesightPreview(
                gameId,
                playerId,
                eraNumber,
                eraNumber + 1,
                List.of(new ForesightPreviewEvent(
                        UUID.randomUUID(),
                        "Event",
                        List.of(new ForesightPreviewOutcome(UUID.randomUUID(), "Outcome", 50)))),
                emptyReason);
    }
}
