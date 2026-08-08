package io.github.temporalrift.read.projection.domain.model;

/** The terminal origin of an event carried into the current era. */
public enum CarryOverState {
    FRESH,
    CASCADED,
    STALLED
}
