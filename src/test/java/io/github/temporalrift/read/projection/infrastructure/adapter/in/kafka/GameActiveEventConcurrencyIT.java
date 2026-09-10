package io.github.temporalrift.read.projection.infrastructure.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.CarryOverState;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventsDrawnFutureEvent;
import io.github.temporalrift.asyncapi.sessionevents.GeneratedChannelContract.EventsDrawnPayload;
import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract.OutcomeAppliedPayload;
import io.github.temporalrift.read.ReadServiceIntegrationTest;

/**
 * Real transactional proof that the {@code game_projection} lock closes the
 * {@code EventsDrawn}/{@code OutcomeApplied} TOCTOU gap — runs the actual {@link ProjectionEventApplier}
 * methods (no reimplemented logic) on two genuinely concurrent threads. Ordering is forced deterministically
 * (outcomeApplied must hold the lock before eventsDrawn ever starts) and the block is verified by asking
 * Postgres whether eventsDrawn's own backend PID is specifically in a lock-wait state, not by a fixed sleep
 * or an unscoped query that any unrelated session could satisfy.
 */
@ReadServiceIntegrationTest
class GameActiveEventConcurrencyIT {

    @Autowired
    ProjectionEventApplier applier;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void concurrentOutcomeAppliedAndEventsDrawn_neverResurrectsTheResolvedEvent() throws Exception {
        var gameId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var outcomeId = UUID.randomUUID();
        var lockHeld = new CountDownLatch(1);
        var releaseOutcomeApplied = new CountDownLatch(1);
        var eventsDrawnPid = new CompletableFuture<Integer>();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var outcomeApplied =
                    executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        applier.applyOutcomeApplied(
                                new OutcomeAppliedPayload(gameId, 1, eventId, outcomeId, List.of()));
                        // The method has atomically created and locked the previously absent per-game anchor.
                        // The transaction remains open until the competing EventsDrawn is waiting on it.
                        lockHeld.countDown();
                        awaitUninterruptibly(releaseOutcomeApplied);
                    }));

            // Only start eventsDrawn once outcomeApplied provably holds the lock — otherwise the thread
            // pool could schedule eventsDrawn first and the two would never actually race.
            assertThat(lockHeld.await(5, TimeUnit.SECONDS)).isTrue();

            var eventsDrawn =
                    executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        eventsDrawnPid.complete(jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class));
                        applier.applyEventsDrawn(new EventsDrawnPayload(
                                gameId,
                                1,
                                List.of(new EventsDrawnFutureEvent(
                                        eventId, "Title", List.of(), CarryOverState.FRESH))));
                    }));

            // Deterministic proof that this exact session is blocked on the newly-created anchor.
            var pid = eventsDrawnPid.get(5, TimeUnit.SECONDS);
            await().atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                                    "SELECT count(*) FROM pg_stat_activity WHERE pid = ? AND wait_event_type = 'Lock'",
                                    Integer.class,
                                    pid))
                            .isEqualTo(1));

            releaseOutcomeApplied.countDown();

            outcomeApplied.get(10, TimeUnit.SECONDS);
            eventsDrawn.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM game_active_event WHERE game_id = ? AND event_id = ?",
                        Integer.class,
                        gameId,
                        eventId))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM game_resolved_event WHERE game_id = ? AND event_id = ?",
                        Integer.class,
                        gameId,
                        eventId))
                .isEqualTo(1);
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Latch was not released in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
