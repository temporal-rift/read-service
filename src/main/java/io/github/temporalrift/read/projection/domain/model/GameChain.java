package io.github.temporalrift.read.projection.domain.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Game-scoped Weaver chain progress, identical for every player — never carries a player identity.
 * {@code pendingLink} is the outcome the Weaver's latest Thread names, null while there is none.
 * {@code confirmedEventIds} is the event of every confirmed link of the chain, in link order, without repeats.
 */
public record GameChain(
        UUID gameId,
        UUID chainId,
        ChainStatus status,
        int length,
        PendingLink pendingLink,
        List<UUID> confirmedEventIds) {

    public GameChain {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(chainId, "chainId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        confirmedEventIds = confirmedEventIds == null ? List.of() : List.copyOf(new LinkedHashSet<>(confirmedEventIds));
    }

    /** A chain with no pending link and no confirmed link events. */
    public GameChain(UUID gameId, UUID chainId, ChainStatus status, int length) {
        this(gameId, chainId, status, length, null, List.of());
    }

    /** A chain with no confirmed link events. */
    public GameChain(UUID gameId, UUID chainId, ChainStatus status, int length, PendingLink pendingLink) {
        this(gameId, chainId, status, length, pendingLink, List.of());
    }

    /** A chain with no pending link. */
    public GameChain(UUID gameId, UUID chainId, ChainStatus status, int length, List<UUID> confirmedEventIds) {
        this(gameId, chainId, status, length, null, confirmedEventIds);
    }

    /** The same chain with the given pending link, or with none. */
    public GameChain withPendingLink(PendingLink link) {
        return new GameChain(gameId, chainId, status, length, link, confirmedEventIds);
    }

    /** The current-era event and outcome a Thread named and the chain has not yet confirmed. */
    public record PendingLink(UUID eventId, UUID outcomeId) {

        public PendingLink {
            Objects.requireNonNull(eventId, "eventId must not be null");
            Objects.requireNonNull(outcomeId, "outcomeId must not be null");
        }
    }
}
