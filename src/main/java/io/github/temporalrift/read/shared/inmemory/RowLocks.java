package io.github.temporalrift.read.shared.inmemory;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.CannotAcquireLockException;

/** Exclusive row locks held until the owning transaction ends. */
final class RowLocks {

    private final Map<Object, TransactionState> owners = new HashMap<>();

    synchronized void acquire(TransactionState transaction, Object lockKey) {
        try {
            for (var owner = owners.get(lockKey); owner != null && owner != transaction; owner = owners.get(lockKey)) {
                wait();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CannotAcquireLockException("Interrupted while waiting for row lock " + lockKey, e);
        }
        if (owners.put(lockKey, transaction) == null) {
            transaction.heldLocks().add(lockKey);
        }
    }

    synchronized void releaseAll(TransactionState transaction) {
        transaction.heldLocks().forEach(owners::remove);
        transaction.heldLocks().clear();
        notifyAll();
    }
}
