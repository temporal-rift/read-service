package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.CarryOverState;
import io.github.temporalrift.read.projection.domain.model.EventOutcome;
import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;
import io.github.temporalrift.read.projection.domain.port.out.GameActiveEventRepository;

public abstract class GameActiveEventContract extends PortContract {

    protected abstract GameActiveEventRepository port();

    @Test
    void savedEventsAreListedForTheirGameOnly() {
        var event = event("Drought");

        inTransaction(() -> port().save(gameId, event));

        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(event);
        assertThat(inTransaction(() -> port().findByGameId(otherGameId))).isEmpty();
    }

    @Test
    void savingTheSameEventAgainKeepsTheFirstRecord() {
        var first = event("First");
        inTransaction(() -> port().save(gameId, first));

        inTransaction(() -> port().save(
                        gameId, new GameActiveEvent(first.eventId(), "Second", CarryOverState.CASCADED, List.of())));

        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(first);
    }

    @Test
    void eventsAreDeletedIndividuallyOrAllAtOnce() {
        var kept = event("Kept");
        var removed = event("Removed");
        inTransaction(() -> {
            port().save(gameId, kept);
            port().save(gameId, removed);
        });

        inTransaction(() -> port().deleteByGameIdAndEventId(gameId, removed.eventId()));
        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(kept);

        inTransaction(() -> port().deleteByGameId(gameId));
        assertThat(inTransaction(() -> port().findByGameId(gameId))).isEmpty();
    }

    @Test
    void resolutionIsRecordedIndependentlyOfTheActiveEventRow() {
        var eventId = UUID.randomUUID();
        assertThat(inTransaction(() -> port().isResolved(gameId, eventId))).isFalse();

        inTransaction(() -> port().markResolved(gameId, eventId));
        inTransaction(() -> port().markResolved(gameId, eventId));

        assertThat(inTransaction(() -> port().isResolved(gameId, eventId))).isTrue();
        assertThat(inTransaction(() -> port().isResolved(otherGameId, eventId))).isFalse();
    }

    @Test
    void clearingAGamesActiveEventsKeepsItsResolutionMarkers() {
        var event = event("Resolved");
        inTransaction(() -> {
            port().save(gameId, event);
            port().markResolved(gameId, event.eventId());
        });

        inTransaction(() -> port().deleteByGameId(gameId));

        assertThat(inTransaction(() -> port().isResolved(gameId, event.eventId())))
                .isTrue();
    }

    private static GameActiveEvent event(String title) {
        return new GameActiveEvent(
                UUID.randomUUID(),
                title,
                CarryOverState.FRESH,
                List.of(
                        new EventOutcome(UUID.randomUUID(), "Outcome", 60),
                        new EventOutcome(UUID.randomUUID(), "Other", 40)));
    }
}
