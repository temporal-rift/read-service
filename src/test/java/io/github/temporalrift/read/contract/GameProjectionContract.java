package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.GameProjection;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;

public abstract class GameProjectionContract extends PortContract {

    protected abstract GameProjectionRepository port();

    @Test
    void anUnknownGameHasNoProjection() {
        assertThat(inTransaction(() -> port().findByGameId(gameId))).isEmpty();
    }

    @Test
    void aSavedProjectionIsReadBackWithItsFirstRevision() {
        var paradoxId = UUID.randomUUID();
        var expiresAt = Instant.parse("2026-08-15T00:00:00Z");

        inTransaction(
                () -> port().save(projection(gameId, 1, Phase.ACTION_ROUND_1, List.of(paradoxId), 20, expiresAt)));

        var read = inTransaction(() -> port().findByGameId(gameId)).orElseThrow();
        assertThat(read.eraNumber()).isEqualTo(1);
        assertThat(read.phase()).isEqualTo(Phase.ACTION_ROUND_1);
        assertThat(read.pendingParadoxIds()).containsExactly(paradoxId);
        assertThat(read.winScoreThreshold()).isEqualTo(20);
        assertThat(read.actionRoundExpiresAt()).isEqualTo(expiresAt);
        assertThat(read.revision()).isZero();
        assertThat(read.lastUpdatedAt()).isNotNull();
    }

    @Test
    void everySaveAdvancesTheRevisionAndAMissingThresholdKeepsTheStoredOne() {
        inTransaction(() -> port().save(projection(gameId, 1, Phase.ERA_START, List.of(), 20, null)));

        inTransaction(() -> port().save(projection(gameId, 2, Phase.RESOLUTION, List.of(), null, null)));

        var read = inTransaction(() -> port().findByGameId(gameId)).orElseThrow();
        assertThat(read.revision()).isEqualTo(1);
        assertThat(read.eraNumber()).isEqualTo(2);
        assertThat(read.phase()).isEqualTo(Phase.RESOLUTION);
        assertThat(read.winScoreThreshold()).isEqualTo(20);
    }

    @Test
    void lockingAnUnknownGameCreatesTheAnchorProjection() {
        var anchor = inTransaction(() -> port().findByGameIdForUpdate(gameId)).orElseThrow();

        assertThat(anchor.gameId()).isEqualTo(gameId);
        assertThat(anchor.eraNumber()).isZero();
        assertThat(anchor.phase()).isEqualTo(Phase.LOBBY);
        assertThat(anchor.revision()).isZero();
        assertThat(inTransaction(() -> port().findByGameId(gameId))).isPresent();
    }

    @Test
    void lockingAKnownGameReadsItsProjectionUnchanged() {
        inTransaction(() -> port().save(projection(gameId, 3, Phase.RESOLUTION, List.of(), 25, null)));

        var locked = inTransaction(() -> port().findByGameIdForUpdate(gameId)).orElseThrow();

        assertThat(locked.eraNumber()).isEqualTo(3);
        assertThat(locked.winScoreThreshold()).isEqualTo(25);
    }

    @Test
    void aSecondTransactionLockingTheSameGameWaitsForTheFirstToEnd() throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var first = CompletableFuture.runAsync(() -> inTransaction(() -> {
            port().findByGameIdForUpdate(gameId);
            locked.countDown();
            await(release);
        }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        var second = CompletableFuture.runAsync(() -> inTransaction(() -> port().findByGameIdForUpdate(gameId)));

        try {
            second.get(1, TimeUnit.SECONDS);
            throw new AssertionError("The second transaction should still be waiting for the lock");
        } catch (TimeoutException expected) {
            release.countDown();
        }
        second.get(10, TimeUnit.SECONDS);
        first.get(10, TimeUnit.SECONDS);
    }

    @Test
    void lockingOneGameDoesNotBlockAnother() throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var first = CompletableFuture.runAsync(() -> inTransaction(() -> {
            port().findByGameIdForUpdate(gameId);
            locked.countDown();
            await(release);
        }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        CompletableFuture.runAsync(() -> inTransaction(() -> port().findByGameIdForUpdate(otherGameId)))
                .get(10, TimeUnit.SECONDS);

        release.countDown();
        first.get(10, TimeUnit.SECONDS);
    }

    private static GameProjection projection(
            UUID gameId, int era, Phase phase, List<UUID> paradoxes, Integer threshold, Instant expiresAt) {
        return new GameProjection(
                gameId, era, phase, paradoxes, null, 1, expiresAt, null, 0, Instant.now(), List.of(), threshold);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
