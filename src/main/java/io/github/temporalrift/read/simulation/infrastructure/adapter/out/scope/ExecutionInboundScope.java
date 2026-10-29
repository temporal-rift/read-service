package io.github.temporalrift.read.simulation.infrastructure.adapter.out.scope;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.github.temporalrift.read.shared.InboundRecord;
import io.github.temporalrift.read.shared.InboundScope;
import io.github.temporalrift.read.simulation.domain.port.out.EventFence;
import io.github.temporalrift.read.simulation.domain.port.out.ExecutionRepository;

/**
 * Admits only records of the game the open context hosts. read-service learns that game from its records, so the
 * first admitted record of a context binds it. Records already in their source when the context opened stay fenced,
 * and games of closed contexts never bind a later one, so a closed game's late records never reach the next game.
 *
 * <p>Every decision is taken holding the execution until the handler's transaction ends. An open captures its fence
 * before its context commits, so a record checked under the lock meets the fence of the context it would act on. A
 * close takes the same lock exclusively, so it either waits for a handler that already passed this check or makes a
 * later handler find no context; a handler can never write into the projections a close just discarded.
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
        if (gameId == null) {
            return false;
        }
        var seen = executions.findScope().orElse(null);
        if (seen == null) {
            return false;
        }
        // Only the lock to take is chosen without it: a context with no game yet is held exclusively, so its first
        // binders queue on one lock instead of each holding a shared lock the other one needs to upgrade.
        var binding = seen.gameId() == null;
        var scope = (binding ? executions.findScopeForBinding() : executions.findScopeForHandling()).orElse(null);
        if (scope == null || isFenced(inbound)) {
            return false;
        }
        if (scope.gameId() != null) {
            return gameId.equals(scope.gameId());
        }
        // A context opened since the unlocked read fenced every record already in the log, this one included.
        if (!binding || executions.isClosedGame(gameId)) {
            return false;
        }
        executions.bindGame(scope.caseKey(), gameId);
        return true;
    }

    private boolean isFenced(InboundRecord inbound) {
        return inbound.topic() != null
                && inbound.partition() != null
                && inbound.offset() != null
                && eventFence.isFenced(inbound.topic(), inbound.partition(), inbound.offset());
    }
}
