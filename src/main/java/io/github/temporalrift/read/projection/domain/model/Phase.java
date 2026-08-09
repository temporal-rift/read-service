package io.github.temporalrift.read.projection.domain.model;

/** The core game flow this slice tracks. */
public enum Phase {
    LOBBY,
    ERA_START,
    ACTION_ROUND_1,
    ACTION_ROUND_2,
    ACTION_ROUND_3,
    PARADOX_RESOLUTION,
    RESOLUTION,
    ERA_END,
    GAME_ENDED
}
