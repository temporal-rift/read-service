package io.github.temporalrift.read.shared;

/**
 * Which inbound records this deployment acts on. A record outside the scope is consumed and ignored: no consumer
 * claims it or changes any state for it.
 */
public interface InboundScope {

    boolean admits(InboundRecord inbound);
}
