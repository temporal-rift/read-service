package io.github.temporalrift.read.shared;

import java.util.UUID;

/** Whether the read side has applied a game's end. */
public interface GameEndProbe {

    boolean hasEnded(UUID gameId);
}
