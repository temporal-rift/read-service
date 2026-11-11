package io.github.temporalrift.read.projection.domain.model;

/**
 * Whether one faction special can be used in a round. {@code unavailableReason} is non-null exactly when
 * {@code available} is false. Both names are the owner's opaque values, like a faction name.
 */
public record SpecialAvailability(String specialAction, boolean available, String unavailableReason) {}
