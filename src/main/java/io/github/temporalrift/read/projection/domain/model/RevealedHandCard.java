package io.github.temporalrift.read.projection.domain.model;

import java.util.UUID;

/** One card sampled from an observed player's hand by an INTERCEPT reveal. */
public record RevealedHandCard(UUID cardInstanceId, String cardType, String grade) {}
