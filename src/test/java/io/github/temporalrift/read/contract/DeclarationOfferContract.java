package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.DeclarationOffer;
import io.github.temporalrift.read.projection.domain.model.DeclarationOffer.Mode;
import io.github.temporalrift.read.projection.domain.port.out.DeclarationOfferRepository;

public abstract class DeclarationOfferContract extends PortContract {

    private final UUID playerId = UUID.randomUUID();

    protected abstract DeclarationOfferRepository port();

    @Test
    void aSavedOfferKeepsItsEligibleModesInOrder() {
        var offer = new DeclarationOffer(gameId, playerId, 2, List.of(Mode.MOMENTUM, Mode.RALLY));

        inTransaction(() -> port().saveIfAbsent(offer));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2)))
                .contains(offer);
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 3)))
                .isEmpty();
    }

    @Test
    void theFirstRecordedOfferIsKept() {
        var first = new DeclarationOffer(gameId, playerId, 2, List.of(Mode.RALLY));
        inTransaction(() -> port().saveIfAbsent(first));

        inTransaction(() -> port().saveIfAbsent(new DeclarationOffer(gameId, playerId, 2, List.of(Mode.MOMENTUM))));

        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2)))
                .contains(first);
    }

    @Test
    void deletesRemoveOnlyTheRequestedScope() {
        var era1 = new DeclarationOffer(gameId, playerId, 1, List.of(Mode.RALLY));
        var era2 = new DeclarationOffer(gameId, playerId, 2, List.of(Mode.RALLY));
        var elsewhere = new DeclarationOffer(otherGameId, playerId, 1, List.of(Mode.RALLY));
        inTransaction(() -> {
            port().saveIfAbsent(era1);
            port().saveIfAbsent(era2);
            port().saveIfAbsent(elsewhere);
        });

        inTransaction(() -> port().deleteByGameIdAndEraNumber(gameId, 1));
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 1)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2)))
                .contains(era2);

        inTransaction(() -> port().deleteByGameId(gameId));
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2)))
                .isEmpty();
        assertThat(inTransaction(() -> port().findByGameIdAndPlayerIdAndEraNumber(otherGameId, playerId, 1)))
                .contains(elsewhere);
    }
}
