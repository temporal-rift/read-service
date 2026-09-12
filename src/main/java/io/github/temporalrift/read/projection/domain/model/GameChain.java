package io.github.temporalrift.read.projection.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Game-scoped Weaver chain progress, identical for every player — never carries a player identity. */
public record GameChain(UUID gameId, UUID chainId, ChainStatus status, int length) {

    public GameChain {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(chainId, "chainId must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
