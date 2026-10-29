package io.github.temporalrift.read.simulation.domain.port.out;

/**
 * Fences inbound records that were already in their source when an execution context closed. Such a record belongs to
 * the closed context whatever its game, so it must never act on a later one, including a replay of the same case.
 */
public interface EventFence {

    /** Whether {@code offset} in {@code topic}/{@code partition} predates the last close. */
    boolean isFenced(String topic, int partition, long offset);

    /** Captures the end of every source, so every record already present is fenced. */
    void capture();
}
