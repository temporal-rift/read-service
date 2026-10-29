package io.github.temporalrift.read.simulation.application.command;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

/**
 * Serialises the operator's open and close operations on one service instance, so the source fence an open captures
 * cannot interleave with another open or close. Each simulation lane hosts one service instance.
 */
@Component
class SimulationControlLock {

    private final ReentrantLock lock = new ReentrantLock();

    void lock() {
        lock.lock();
    }

    void unlock() {
        lock.unlock();
    }
}
