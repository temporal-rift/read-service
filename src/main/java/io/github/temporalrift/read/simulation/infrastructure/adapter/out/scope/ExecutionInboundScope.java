package io.github.temporalrift.read.simulation.infrastructure.adapter.out.scope;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.github.temporalrift.read.shared.InboundRecord;
import io.github.temporalrift.read.shared.InboundScope;
import io.github.temporalrift.read.simulation.domain.port.out.EventFence;
import io.github.temporalrift.read.simulation.domain.port.out.ExecutionRepository;

/**
 * Admits only records of the game the open context hosts. read-service learns that game from its records, so the
 * first admitted record of a context binds it. Records already in their source when the context opened stay fenced, and
 * games of closed contexts never bind a later one, so a closed game's late records never reach the next game.
 *
 * <p>The scope is read with a lock held until the handler's transaction ends. A close takes the same lock exclusively,
 * so it either waits for a handler that already passed this check or makes a later handler find no context; a handler
 * can never write into the projections a close just discarded.
 */
@Component
@ConditionalOnProperty(name = "game.simulation.enabled", havingValue = "true")
class ExecutionInboundScope implements InboundScope {

    private final ExecutionRepository executions;
    private final EventFence eventFence;

    ExecutionInboundScope(ExecutionRepository executions, EventFence eventFence) {
        this.executions = executions;
        this.eventFence = eventFence;
    }

    @Override
    public boolean admits(InboundRecord inbound) {
        var gameId = inbound.gameId();
        if (gameId == null || isFenced(inbound)) {
            return false;
        }
        var seen = executions.findScope().orElse(null);
        if (seen == null) {
            return false;
        }
        if (seen.gameId() == null && !executions.isClosedGame(gameId)) {
            // Bound before the handling lock is taken, so concurrent binders queue on the row's write lock instead of
            // each holding a shared lock the other one needs to upgrade.
            executions.bindGame(seen.caseKey(), gameId);
        }
        var scope = executions.findScopeForHandling().orElse(null);
        return scope != null && gameId.equals(scope.gameId());
    }

    private boolean isFenced(InboundRecord inbound) {
        return inbound.topic() != null
                && inbound.partition() != null
                && inbound.offset() != null
                && eventFence.isFenced(inbound.topic(), inbound.partition(), inbound.offset());
    }
}
