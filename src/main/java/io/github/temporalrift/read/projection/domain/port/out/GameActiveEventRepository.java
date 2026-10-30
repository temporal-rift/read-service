package io.github.temporalrift.read.projection.domain.port.out;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;

public interface GameActiveEventRepository {

    /** The game's active events in reveal order. */
    List<GameActiveEvent> findByGameId(UUID gameId);

    /** Records the event as drawn now, replacing the facts of a row the game still holds for the same event. */
    void save(UUID gameId, GameActiveEvent gameActiveEvent);

    void deleteByGameIdAndEventId(UUID gameId, UUID eventId);

    /** Defensive clear on era end. */
    void deleteByGameId(UUID gameId);

    /** True once {@code OutcomeApplied} has resolved this event, regardless of cross-topic arrival order. */
    boolean isResolved(UUID gameId, UUID eventId);

    /** Records resolution independently of whether the active-event row exists yet. */
    void markResolved(UUID gameId, UUID eventId);
}
