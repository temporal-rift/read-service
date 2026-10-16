package io.github.temporalrift.read.shared.inmemory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writes, observed versions and row locks of one in-memory transaction. */
final class TransactionState {

    private final Map<Table<?, ?>, Map<Object, Map<RowKey, Pending<?>>>> pending = new LinkedHashMap<>();
    private final Map<Table<?, ?>, Map<Object, Map<RowKey, Long>>> observedVersions = new HashMap<>();
    private final List<Object> heldLocks = new ArrayList<>();
    private final Thread thread = Thread.currentThread();
    private boolean rollbackOnly;

    <V> Map<RowKey, Pending<V>> pendingFor(Table<?, V> table, Object partition) {
        @SuppressWarnings("unchecked")
        var rows = (Map<RowKey, Pending<V>>) (Map<RowKey, ?>) pending.computeIfAbsent(table, t -> new HashMap<>())
                .computeIfAbsent(partition, p -> new LinkedHashMap<>());
        return rows;
    }

    Map<Table<?, ?>, Map<Object, Map<RowKey, Pending<?>>>> allPending() {
        return pending;
    }

    Map<RowKey, Long> observedVersionsFor(Table<?, ?> table, Object partition) {
        return observedVersions
                .computeIfAbsent(table, t -> new HashMap<>())
                .computeIfAbsent(partition, p -> new HashMap<>());
    }

    List<Object> heldLocks() {
        return heldLocks;
    }

    Thread thread() {
        return thread;
    }

    boolean isRollbackOnly() {
        return rollbackOnly;
    }

    void markRollbackOnly() {
        rollbackOnly = true;
    }

    enum Kind {
        INSERT,
        UPDATE,
        DELETE
    }

    /** A buffered write; {@code value} is null for a delete. */
    record Pending<V>(Kind kind, V value, long expectedVersion) {}
}
