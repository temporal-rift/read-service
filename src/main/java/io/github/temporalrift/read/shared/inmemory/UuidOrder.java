package io.github.temporalrift.read.shared.inmemory;

import java.util.Comparator;
import java.util.UUID;

/** UUID ordering as PostgreSQL sorts a {@code uuid} column: bytewise, so unsigned per 64-bit half. */
public final class UuidOrder {

    public static final Comparator<UUID> ASCENDING = Comparator.<UUID>comparingLong(
                    uuid -> uuid.getMostSignificantBits() ^ Long.MIN_VALUE)
            .thenComparingLong(uuid -> uuid.getLeastSignificantBits() ^ Long.MIN_VALUE);

    private UuidOrder() {}
}
