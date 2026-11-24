package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.ChainStatus;
import io.github.temporalrift.read.projection.domain.model.GameChain;
import io.github.temporalrift.read.projection.domain.port.out.GameChainRepository;

public abstract class GameChainContract extends PortContract {

    protected abstract GameChainRepository port();

    @Test
    void aGameWithoutAChainHasNone() {
        assertThat(inTransaction(() -> port().findByGameId(gameId))).isEmpty();
    }

    @Test
    void aSavedChainIsReplacedByTheNextSave() {
        var chainId = UUID.randomUUID();
        inTransaction(() -> port().save(gameId, new GameChain(gameId, chainId, ChainStatus.ACTIVE, 1)));

        inTransaction(() -> port().save(gameId, new GameChain(gameId, chainId, ChainStatus.COMPLETED, 3)));

        assertThat(inTransaction(() -> port().findByGameId(gameId)))
                .contains(new GameChain(gameId, chainId, ChainStatus.COMPLETED, 3));
        assertThat(inTransaction(() -> port().findByGameId(otherGameId))).isEmpty();
    }

    @Test
    void aPendingLinkIsStoredAndClearedByTheNextSave() {
        var chainId = UUID.randomUUID();
        var link = new GameChain.PendingLink(UUID.randomUUID(), UUID.randomUUID());
        inTransaction(() -> port().save(gameId, new GameChain(gameId, chainId, ChainStatus.ACTIVE, 2, link)));

        assertThat(inTransaction(() -> port().findByGameId(gameId)))
                .contains(new GameChain(gameId, chainId, ChainStatus.ACTIVE, 2, link));

        inTransaction(() -> port().save(gameId, new GameChain(gameId, chainId, ChainStatus.ACTIVE, 3)));

        assertThat(inTransaction(() -> port().findByGameId(gameId)))
                .contains(new GameChain(gameId, chainId, ChainStatus.ACTIVE, 3));
    }

    @Test
    void confirmedLinkEventsAreStoredInLinkOrderAndReplacedByTheNextSave() {
        var chainId = UUID.randomUUID();
        // HIGH sorts after LOW in every uuid order, so a store that sorted the ids would read them back reordered.
        inTransaction(
                () -> port().save(gameId, new GameChain(gameId, chainId, ChainStatus.ACTIVE, 2, List.of(HIGH, LOW))));

        assertThat(inTransaction(() -> port().findByGameId(gameId)))
                .contains(new GameChain(gameId, chainId, ChainStatus.ACTIVE, 2, List.of(HIGH, LOW)));

        inTransaction(() -> port().save(
                        gameId,
                        new GameChain(
                                gameId,
                                chainId,
                                ChainStatus.COMPLETED,
                                3,
                                new GameChain.PendingLink(UUID.randomUUID(), UUID.randomUUID()),
                                List.of(HIGH))));

        var stored = inTransaction(() -> port().findByGameId(gameId)).orElseThrow();
        assertThat(stored.confirmedEventIds()).containsExactly(HIGH);
    }

    @Test
    void aDeletedChainIsGone() {
        inTransaction(() -> port().save(gameId, new GameChain(gameId, UUID.randomUUID(), ChainStatus.BROKEN, 2)));

        inTransaction(() -> port().deleteByGameId(gameId));

        assertThat(inTransaction(() -> port().findByGameId(gameId))).isEmpty();
    }
}
