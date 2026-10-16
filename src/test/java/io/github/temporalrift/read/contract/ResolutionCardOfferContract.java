package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.ResolutionCardOffer;
import io.github.temporalrift.read.projection.domain.port.out.ResolutionCardOfferRepository;

public abstract class ResolutionCardOfferContract extends PortContract {

    private final UUID playerId = UUID.randomUUID();

    protected abstract ResolutionCardOfferRepository port();

    @Test
    void anOfferIsReadBackForItsPlayerAndEra() {
        var offer = offer(1, "PUSH");

        inTransaction(() -> port().upsert(offer));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 1)))
                .contains(offer);
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2)))
                .isEmpty();
    }

    @Test
    void upsertReplacesTheOfferOfTheSamePlayerAndEra() {
        inTransaction(() -> port().upsert(offer(1, "PUSH")));

        var replacement = offer(1, "SCAN");
        inTransaction(() -> port().upsert(replacement));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 1)))
                .contains(replacement);
    }

    @Test
    void deletesRemoveOnlyTheRequestedScope() {
        var era2 = offer(2, "PUSH");
        inTransaction(() -> {
            port().upsert(offer(1, "PUSH"));
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

    private ResolutionCardOffer offer(int eraNumber, String cardType) {
        return new ResolutionCardOffer(
                gameId, playerId, eraNumber, List.of(new ResolutionCardOffer.Card(UUID.randomUUID(), cardType, "II")));
    }
}
