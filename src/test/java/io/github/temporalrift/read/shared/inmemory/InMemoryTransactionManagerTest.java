package io.github.temporalrift.read.shared.inmemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class InMemoryTransactionManagerTest {

    private static final RowKey ROW = RowKey.of("row");

    private final InMemoryDatabase database = new InMemoryDatabase();
    private final Table<UUID, String> rows = database.table("rows", false);
    private final Table<UUID, String> versioned = database.table("versioned_rows", true);
    private final TransactionTemplate transaction = new TransactionTemplate(new InMemoryTransactionManager(database));
    private final UUID game = UUID.randomUUID();

    @Test
    void committedWritesAreVisibleAfterwards() {
        transaction.executeWithoutResult(status -> rows.put(game, ROW, "committed"));

        assertThat(rows.find(game, ROW)).contains("committed");
    }

    @Test
    void rollbackDiscardsEveryWriteOfTheTransaction() {
        rows.put(game, ROW, "before");

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                    rows.put(game, ROW, "after");
                    rows.put(game, RowKey.of("other"), "new");
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(rows.find(game, ROW)).contains("before");
        assertThat(rows.find(game, RowKey.of("other"))).isEmpty();
    }

    @Test
    void uncommittedWritesAreInvisibleToOtherTransactionsButVisibleToTheWriter() throws Exception {
        transaction.executeWithoutResult(status -> {
            rows.put(game, ROW, "pending");

            assertThat(rows.find(game, ROW)).contains("pending");
            assertThat(CompletableFuture.supplyAsync(() -> rows.find(game, ROW)).join())
                    .isEmpty();
        });
        assertThat(rows.find(game, ROW)).contains("pending");
    }

    @Test
    void aWriterReadsItsOwnDeletesAndReinserts() {
        rows.put(game, ROW, "first");

        transaction.executeWithoutResult(status -> {
            rows.remove(game, ROW);
            assertThat(rows.find(game, ROW)).isEmpty();
            rows.put(game, ROW, "second");
            assertThat(rows.list(game)).containsExactly("second");
        });

        assertThat(rows.list(game)).containsExactly("second");
    }

    @Test
    void rowsKeepInsertionOrderAcrossReplacement() {
        rows.put(game, RowKey.of("a"), "a");
        rows.put(game, RowKey.of("b"), "b");

        transaction.executeWithoutResult(status -> {
            rows.put(game, RowKey.of("a"), "a2");
            rows.put(game, RowKey.of("c"), "c");
        });

        assertThat(rows.list(game)).containsExactly("a2", "b", "c");
    }

    @Test
    void requiresNewRunsInAnIndependentTransaction() {
        var inner = new TransactionTemplate(transaction.getTransactionManager());
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                    rows.put(game, ROW, "outer");
                    inner.executeWithoutResult(innerStatus -> {
                        assertThat(rows.find(game, ROW)).isEmpty();
                        rows.put(game, RowKey.of("inner"), "inner");
                    });
                    throw new IllegalStateException("outer fails");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(rows.find(game, RowKey.of("inner"))).contains("inner");
        assertThat(rows.find(game, ROW)).isEmpty();
    }

    @Test
    void aConcurrentDuplicateInsertFailsAtCommit() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                    rows.put(game, ROW, "mine");
                    CompletableFuture.runAsync(() -> rows.put(game, ROW, "theirs"))
                            .join();
                }))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(rows.find(game, ROW)).contains("theirs");
    }

    @Test
    void aStaleWriteToAVersionedTableFailsAtCommit() {
        versioned.put(game, ROW, "v0");

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                    var read = versioned.find(game, ROW).orElseThrow();
                    CompletableFuture.runAsync(() -> versioned.put(game, ROW, "concurrent"))
                            .join();
                    versioned.put(game, ROW, read + "-updated");
                }))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(versioned.find(game, ROW)).contains("concurrent");
    }

    @Test
    void aSecondLockerWaitsForTheFirstToEnd() throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var first = CompletableFuture.runAsync(() -> transaction.executeWithoutResult(status -> {
            rows.lock(game, ROW);
            locked.countDown();
            await(release);
        }));
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();

        var second = CompletableFuture.runAsync(() -> transaction.executeWithoutResult(status -> rows.lock(game, ROW)));

        assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
                .isInstanceOf(java.util.concurrent.TimeoutException.class);
        release.countDown();
        second.get(5, TimeUnit.SECONDS);
        first.get(5, TimeUnit.SECONDS);
    }

    @Test
    void aLockIsReleasedWhenTheHolderRollsBack() throws Exception {
        var failed = CompletableFuture.runAsync(() -> transaction.executeWithoutResult(status -> {
            rows.lock(game, ROW);
            throw new IllegalStateException("boom");
        }));
        assertThatThrownBy(failed::get).isInstanceOf(ExecutionException.class);

        CompletableFuture.runAsync(() -> transaction.executeWithoutResult(status -> rows.lock(game, ROW)))
                .get(5, TimeUnit.SECONDS);
    }

    @Test
    void lockingOutsideATransactionIsRejected() {
        assertThatThrownBy(() -> rows.lock(game, ROW)).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void afterCommitSynchronisationsRunOnCommitOnly() {
        var events = new ArrayList<String>();

        transaction.executeWithoutResult(status -> register(events, "committed"));
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                    register(events, "rolled back");
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(events).containsExactly("committed");
    }

    @Test
    void writesOutsideATransactionCommitImmediately() {
        rows.put(game, ROW, "direct");
        rows.removePartition(game);

        assertThat(rows.list(game)).isEmpty();
    }

    private static void register(List<String> events, String name) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.add(name);
            }
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
