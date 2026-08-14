package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/** A card in the player's private, unresolved hand-selection pool. */
public record PendingHandCard(UUID cardInstanceId, String cardType, String grade, int dealSlot) {}
