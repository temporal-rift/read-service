package io.github.temporalrift.read.shared.inmemory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import io.github.temporalrift.read.shared.inmemory.TransactionState.Kind;
import io.github.temporalrift.read.shared.inmemory.TransactionState.Pending;

/**
 * A table of immutable rows partitioned by an owner key (the game id), read through the current transaction overlay
 * and written only to that overlay until commit. Rows keep insertion order; a versioned table additionally rejects a
 * write based on a stale read, like an optimistic-lock column.
 */
public final class Table<P, V> {

    private final InMemoryDatabase database;
    private final String name;
    private final boolean versioned;
    private final Map<P, LinkedHashMap<RowKey, Row<V>>> committed = new HashMap<>();

    Table(InMemoryDatabase database, String name, boolean versioned) {
        this.database = database;
        this.name = name;
        this.versioned = versioned;
    }

    boolean isVersioned() {
        return versioned;
    }

    public Optional<V> find(P partition, RowKey key) {
        return database.inTransaction(tx -> visibleRows(tx, partition).stream()
                .filter(entry -> entry.key().equals(key))
                .map(Entry::value)
                .findFirst());
    }

    public List<V> list(P partition) {
        return list(partition, value -> true);
    }

    public List<V> list(P partition, Predicate<V> filter) {
        return database.inTransaction(tx -> visibleRows(tx, partition).stream()
                .map(Entry::value)
                .filter(filter)
                .toList());
    }

    /** Inserts the row, or replaces the row with the same key. */
    public void put(P partition, RowKey key, V value) {
        database.inTransaction(tx -> {
            putIn(tx, partition, key, value);
            return null;
        });
    }

    /** @return true when the row was absent and has been inserted */
    public boolean putIfAbsent(P partition, RowKey key, V value) {
        return database.inTransaction(tx -> {
            if (visibleRows(tx, partition).stream()
                    .anyMatch(entry -> entry.key().equals(key))) {
                return false;
            }
            putIn(tx, partition, key, value);
            return true;
        });
    }

    public void remove(P partition, RowKey key) {
        database.inTransaction(tx -> {
            removeIn(tx, partition, key);
            return null;
        });
    }

    public void removeIf(P partition, Predicate<V> filter) {
        database.inTransaction(tx -> {
            visibleRows(tx, partition).stream()
                    .filter(entry -> filter.test(entry.value()))
                    .map(Entry::key)
                    .forEach(key -> removeIn(tx, partition, key));
            return null;
        });
    }

    public void removePartition(P partition) {
        removeIf(partition, value -> true);
    }

    /** Holds an exclusive lock on the row identity, present or not, until the transaction ends. */
    public void lock(P partition, RowKey key) {
        database.locks().acquire(database.requireTransaction(), new LockId(name, partition, key));
    }

    private void putIn(TransactionState tx, P partition, RowKey key, V value) {
        visibleRows(tx, partition);
        var pending = tx.<V>pendingFor(this, partition);
        var previous = pending.get(key);
        if (previous == null) {
            pending.put(key, firstWrite(tx, partition, key, value));
        } else if (previous.kind() == Kind.INSERT) {
            pending.put(key, new Pending<>(Kind.INSERT, value, 0));
        } else {
            pending.put(key, new Pending<>(Kind.UPDATE, value, previous.expectedVersion()));
        }
    }

    private void removeIn(TransactionState tx, P partition, RowKey key) {
        visibleRows(tx, partition);
        var pending = tx.<V>pendingFor(this, partition);
        var previous = pending.get(key);
        if (previous == null) {
            committedVersion(tx, partition, key)
                    .ifPresent(version -> pending.put(key, new Pending<>(Kind.DELETE, null, version)));
        } else if (previous.kind() == Kind.INSERT) {
            pending.remove(key);
        } else {
            pending.put(key, new Pending<>(Kind.DELETE, null, previous.expectedVersion()));
        }
    }

    private Pending<V> firstWrite(TransactionState tx, P partition, RowKey key, V value) {
        return committedVersion(tx, partition, key)
                .map(version -> new Pending<>(Kind.UPDATE, value, version))
                .orElseGet(() -> new Pending<>(Kind.INSERT, value, 0));
    }

    private List<Entry<V>> visibleRows(TransactionState tx, P partition) {
        var base = database.read(() -> snapshot(partition));
        var pending = tx.<V>pendingFor(this, partition);
        if (versioned) {
            var observed = tx.observedVersionsFor(this, partition);
            base.forEach(row -> observed.putIfAbsent(row.key(), row.version()));
        }
        var rows = new ArrayList<Entry<V>>();
        var baseKeys = new HashSet<RowKey>();
        for (var row : base) {
            baseKeys.add(row.key());
            var change = pending.get(row.key());
            if (change == null) {
                rows.add(new Entry<>(row.key(), row.value()));
            } else if (change.kind() != Kind.DELETE) {
                rows.add(new Entry<>(row.key(), change.value()));
            }
        }
        pending.forEach((key, change) -> {
            if (!baseKeys.contains(key) && change.kind() != Kind.DELETE) {
                rows.add(new Entry<>(key, change.value()));
            }
        });
        return rows;
    }

    private Optional<Long> committedVersion(TransactionState tx, P partition, RowKey key) {
        var observed = tx.observedVersionsFor(this, partition).get(key);
        if (observed != null) {
            return Optional.of(observed);
        }
        return database.read(() -> snapshot(partition).stream()
                .filter(row -> row.key().equals(key))
                .map(SnapshotRow::version)
                .findFirst());
    }

    private List<SnapshotRow<V>> snapshot(P partition) {
        var rows = committed.get(partition);
        if (rows == null) {
            return List.of();
        }
        return rows.entrySet().stream()
                .map(e -> new SnapshotRow<>(
                        e.getKey(), e.getValue().value(), e.getValue().version()))
                .toList();
    }

    /** Checks a transaction's writes against committed rows; the caller holds the store write lock. */
    @SuppressWarnings("unchecked")
    void validate(Map<Object, Map<RowKey, Pending<?>>> writes) {
        writes.forEach((partition, rows) -> rows.forEach((key, change) -> {
            var existing = rowOf((P) partition, key);
            if (change.kind() == Kind.INSERT && existing != null) {
                throw new DataIntegrityViolationException("Duplicate key " + key + " in " + name);
            }
            if (versioned
                    && change.kind() != Kind.INSERT
                    && (existing == null || existing.version() != change.expectedVersion())) {
                throw new ObjectOptimisticLockingFailureException(name, key);
            }
        }));
    }

    /** Applies a validated transaction's writes; the caller holds the store write lock. */
    @SuppressWarnings("unchecked")
    void apply(Map<Object, Map<RowKey, Pending<?>>> writes) {
        writes.forEach((partition, rows) -> rows.forEach((key, change) -> {
            var partitionRows = committed.computeIfAbsent((P) partition, p -> new LinkedHashMap<>());
            if (change.kind() == Kind.DELETE) {
                partitionRows.remove(key);
            } else {
                var existing = partitionRows.get(key);
                partitionRows.put(key, new Row<>((V) change.value(), existing == null ? 0 : existing.version() + 1));
            }
            if (partitionRows.isEmpty()) {
                committed.remove(partition);
            }
        }));
    }

    private Row<V> rowOf(P partition, RowKey key) {
        var rows = committed.get(partition);
        return rows == null ? null : rows.get(key);
    }

    private record Row<V>(V value, long version) {}

    private record SnapshotRow<V>(RowKey key, V value, long version) {}

    private record Entry<V>(RowKey key, V value) {}

    private record LockId(String table, Object partition, RowKey key) {}
}
