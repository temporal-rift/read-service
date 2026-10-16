package io.github.temporalrift.read.contract;

import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.transaction.support.TransactionOperations;

/**
 * Base of the driven-port contract suite. A contract holds the assertions once; a subclass per adapter supplies the
 * port and the transaction boundary, so the production and the in-memory adapter cannot drift apart.
 */
public abstract class PortContract {

    /** Sorts first under PostgreSQL's bytewise uuid order and under Java's signed order alike. */
    protected static final UUID LOW = new UUID(1L, 1L);

    /** Sorts last under PostgreSQL's bytewise uuid order but first under Java's signed {@link UUID} order. */
    protected static final UUID HIGH = new UUID(-1L, 1L);

    protected final UUID gameId = UUID.randomUUID();
    protected final UUID otherGameId = UUID.randomUUID();

    protected abstract TransactionOperations transactions();

    protected void inTransaction(Runnable work) {
        transactions().executeWithoutResult(status -> work.run());
    }

    protected <T> T inTransaction(Supplier<T> work) {
        return transactions().execute(status -> work.get());
    }
}
