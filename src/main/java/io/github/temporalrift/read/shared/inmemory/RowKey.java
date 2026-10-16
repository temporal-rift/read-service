package io.github.temporalrift.read.shared.inmemory;

import java.util.List;

/** Identity of a row within its game partition, equal when all its parts are equal. */
public record RowKey(List<Object> parts) {

    public RowKey {
        parts = List.copyOf(parts);
    }

    public static RowKey of(Object... parts) {
        return new RowKey(List.of(parts));
    }
}
