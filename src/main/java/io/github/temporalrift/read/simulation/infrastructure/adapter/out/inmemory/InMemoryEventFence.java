package io.github.temporalrift.read.simulation.infrastructure.adapter.out.inmemory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.github.temporalrift.read.simulation.domain.port.out.EventFence;

/**
 * The in-memory runtime has no log: a host hands each record over synchronously as it is published, so no record
 * waits in a source while a context closes. A delivery already in progress is separated from a close by the execution
 * lock rather than by an offset, so nothing is fenced here.
 */
@Component
@Profile("in-memory")
@ConditionalOnProperty(name = "game.simulation.enabled", havingValue = "true")
class InMemoryEventFence implements EventFence {

    @Override
    public boolean isFenced(String topic, int partition, long offset) {
        return false;
    }

    @Override
    public void capture() {
        // No source holds records in this runtime.
    }
}
