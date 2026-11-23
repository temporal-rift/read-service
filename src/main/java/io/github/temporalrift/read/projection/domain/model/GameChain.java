package io.github.temporalrift.read.projection.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Game-scoped Weaver chain progress, identical for every player — never carries a player identity.
 * {@code pendingLink} is the outcome the Weaver's latest Thread names, null while there is none.
 */
public record GameChain(UUID gameId, UUID chainId, ChainStatus status, int length, PendingLink pendingLink) {

    public GameChain {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(chainId, "chainId must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }

    /** A chain with no pending link. */
    public GameChain(UUID gameId, UUID chainId, ChainStatus status, int length) {
        this(gameId, chainId, status, length, null);
    }

    /** The same chain with the given pending link, or with none. */
    public GameChain withPendingLink(PendingLink link) {
        return new GameChain(gameId, chainId, status, length, link);
    }

    /** The current-era event and outcome a Thread named and the chain has not yet confirmed. */
    public record PendingLink(UUID eventId, UUID outcomeId) {

        public PendingLink {
            Objects.requireNonNull(eventId, "eventId must not be null");
            Objects.requireNonNull(outcomeId, "outcomeId must not be null");
        }
    }
}
