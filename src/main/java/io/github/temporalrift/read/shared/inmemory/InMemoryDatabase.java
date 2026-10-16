package io.github.temporalrift.read.shared.inmemory;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The committed rows, row locks and transaction binding behind every in-memory adapter of one application context.
 * Holds no static state, so two contexts in one JVM never share rows.
 */
public class InMemoryDatabase {

    private final ReentrantReadWriteLock storeLock = new ReentrantReadWriteLock();
    private final RowLocks locks = new RowLocks();

    public <P, V> Table<P, V> table(String name, boolean versioned) {
        return new Table<>(this, name, versioned);
    }

    <R> R inTransaction(Function<TransactionState, R> operation) {
        var bound = bound();
        if (bound != null) {
            return operation.apply(bound);
        }
        // Outside a transaction a call commits on its own, as a repository call does.
        var autocommit = new TransactionState();
        try {
            var result = operation.apply(autocommit);
            commit(autocommit);
            return result;
        } finally {
            finish(autocommit);
        }
    }

    TransactionState requireTransaction() {
        var bound = bound();
        if (bound == null) {
            throw new IllegalTransactionStateException("A row lock requires an active transaction");
        }
        return bound;
    }

    RowLocks locks() {
        return locks;
    }

    <R> R read(Supplier<R> reader) {
        storeLock.readLock().lock();
        try {
            return reader.get();
        } finally {
            storeLock.readLock().unlock();
        }
    }

    void commit(TransactionState transaction) {
        storeLock.writeLock().lock();
        try {
            transaction.allPending().forEach(Table::validate);
            transaction.allPending().forEach(Table::apply);
        } finally {
            storeLock.writeLock().unlock();
        }
        transaction.allPending().clear();
    }

    void finish(TransactionState transaction) {
        locks.releaseAll(transaction);
    }

    void bind(TransactionState transaction) {
        TransactionSynchronizationManager.bindResource(this, transaction);
    }

    TransactionState unbind() {
        return (TransactionState) TransactionSynchronizationManager.unbindResource(this);
    }

    TransactionState bound() {
        return (TransactionState) TransactionSynchronizationManager.getResource(this);
    }
}
