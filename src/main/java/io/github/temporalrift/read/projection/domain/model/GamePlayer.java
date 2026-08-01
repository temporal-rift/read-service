package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/** One participant's public state within a game. {@code faction} is null until {@code FactionRevealed}. */
public record GamePlayer(UUID playerId, int score, boolean isConnected, String faction) {}
