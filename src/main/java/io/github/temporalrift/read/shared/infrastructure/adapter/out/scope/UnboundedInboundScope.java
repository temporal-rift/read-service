package io.github.temporalrift.read.shared.infrastructure.adapter.out.scope;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.github.temporalrift.read.shared.InboundRecord;
import io.github.temporalrift.read.shared.InboundScope;

/** An ordinary deployment projects every game it reads. */
@Component
@ConditionalOnProperty(name = "game.simulation.enabled", havingValue = "false", matchIfMissing = true)
class UnboundedInboundScope implements InboundScope {

    @Override
    public boolean admits(InboundRecord inbound) {
        return true;
    }
}
