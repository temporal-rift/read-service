package io.github.temporalrift.read.shared.inmemory;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;

/**
 * Transaction manager for {@link InMemoryDatabase}: writes are visible to others only after commit, a rollback
 * discards them, and the standard synchronisation callbacks run, including after-commit work. Supports
 * {@code REQUIRES_NEW} through suspend and resume; savepoints are not supported.
 */
public class InMemoryTransactionManager extends AbstractPlatformTransactionManager {

    private final InMemoryDatabase database;

    public InMemoryTransactionManager(InMemoryDatabase database) {
        this.database = database;
    }

    @Override
    protected Object doGetTransaction() {
        return new Handle(database.bound());
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) {
        return ((Handle) transaction).state != null;
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        var handle = (Handle) transaction;
        handle.state = new TransactionState();
        database.bind(handle.state);
    }

    @Override
    protected Object doSuspend(Object transaction) {
        ((Handle) transaction).state = null;
        return database.unbind();
    }

    @Override
    protected void doResume(Object transaction, Object suspendedResources) {
        database.bind((TransactionState) suspendedResources);
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        database.commit(((Handle) status.getTransaction()).state);
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        ((Handle) status.getTransaction()).state.allPending().clear();
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        ((Handle) status.getTransaction()).state.markRollbackOnly();
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        var state = ((Handle) transaction).state;
        database.unbind();
        database.finish(state);
    }

    private static final class Handle implements SmartTransactionObject {

        private TransactionState state;

        Handle(TransactionState state) {
            this.state = state;
        }

        @Override
        public boolean isRollbackOnly() {
            return state != null && state.isRollbackOnly();
        }

        @Override
        public void flush() {
            // Writes are buffered until commit; there is nothing to flush.
        }
    }
}
