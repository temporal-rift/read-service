package io.github.temporalrift.read.shared.inmemory;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.CannotAcquireLockException;

/** Exclusive row locks held until the owning transaction ends. */
final class RowLocks {

    private final Map<Object, TransactionState> owners = new HashMap<>();
    private final Map<TransactionState, TransactionState> waitingFor = new HashMap<>();

    synchronized void acquire(TransactionState transaction, Object lockKey) {
        try {
            while (heldByAnother(lockKey, transaction)) {
                var owner = owners.get(lockKey);
                failIfDeadlocked(transaction, owner, lockKey);
                waitingFor.put(transaction, owner);
                wait();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CannotAcquireLockException("Interrupted while waiting for row lock " + lockKey, e);
        } finally {
            waitingFor.remove(transaction);
        }
        if (owners.put(lockKey, transaction) == null) {
            transaction.heldLocks().add(lockKey);
        }
    }

    /**
     * Fails the requester when waiting could never end, as PostgreSQL aborts a deadlock victim: the owner
     * (transitively) waits for this transaction, or runs on this very thread and so cannot finish while it blocks.
     */
    private void failIfDeadlocked(TransactionState transaction, TransactionState owner, Object lockKey) {
        for (var holder = owner; holder != null; holder = waitingFor.get(holder)) {
            if (holder == transaction || holder.thread() == transaction.thread()) {
                throw new CannotAcquireLockException("Deadlock detected while waiting for row lock " + lockKey);
            }
        }
    }

    private boolean heldByAnother(Object lockKey, TransactionState transaction) {
        var owner = owners.get(lockKey);
        return owner != null && owner != transaction;
    }

    synchronized void releaseAll(TransactionState transaction) {
        transaction.heldLocks().forEach(owners::remove);
        transaction.heldLocks().clear();
        notifyAll();
    }
}
