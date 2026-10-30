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
        var event = event(0, "Drought");

        inTransaction(() -> port().save(gameId, event));

        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(event);
        assertThat(inTransaction(() -> port().findByGameId(otherGameId))).isEmpty();
    }

    @Test
    void eventsAreListedInRevealOrderWhateverOrderTheyWereSavedIn() {
        var first = event(0, "First");
        var second = event(1, "Second");
        var third = event(2, "Third");

        inTransaction(() -> {
            port().save(gameId, third);
            port().save(gameId, first);
            port().save(gameId, second);
        });

        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(first, second, third);
    }

    @Test
    void revealOrderDoesNotDependOnWhatTheStoreHeldBefore() {
        // Rows of another game come and go between this game's saves, freeing space a later row may take.
        var first = event(0, "First");
        var second = event(1, "Second");
        var third = event(2, "Third");
        inTransaction(() -> port().save(otherGameId, event(0, "Transient")));
        inTransaction(() -> port().save(gameId, second));
        inTransaction(() -> port().deleteByGameId(otherGameId));
        inTransaction(() -> port().save(gameId, third));
        inTransaction(() -> port().save(otherGameId, event(1, "Transient")));
        inTransaction(() -> port().deleteByGameId(otherGameId));
        inTransaction(() -> port().save(gameId, first));

        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(first, second, third);
        assertThat(inTransaction(() -> port().findByGameId(otherGameId))).isEmpty();
    }

    @Test
    void removingAnEventKeepsTheOthersInRevealOrder() {
        var first = event(0, "First");
        var second = event(1, "Second");
        var third = event(2, "Third");
        inTransaction(() -> {
            port().save(gameId, first);
            port().save(gameId, second);
            port().save(gameId, third);
        });

        inTransaction(() -> port().deleteByGameIdAndEventId(gameId, second.eventId()));

        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(first, third);
    }

    @Test
    void eventsSharingARevealIndexAreOrderedByEventId() {
        var high = new GameActiveEvent(HIGH, 0, "High", CarryOverState.FRESH, List.of());
        var low = new GameActiveEvent(LOW, 0, "Low", CarryOverState.FRESH, List.of());

        inTransaction(() -> {
            port().save(gameId, high);
            port().save(gameId, low);
        });

        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(low, high);
    }

    @Test
    void aLaterDrawReplacesTheFactsOfAnEventTheGameStillHolds() {
        var carried = event(0, "Drought");
        var other = event(0, "Flood");
        inTransaction(() -> port().save(gameId, carried));

        var redrawn = new GameActiveEvent(
                carried.eventId(),
                1,
                "Drought",
                CarryOverState.CASCADED,
                List.of(new EventOutcome(UUID.randomUUID(), "Rain", 70)));
        inTransaction(() -> {
            port().save(gameId, other);
            port().save(gameId, redrawn);
        });

        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(other, redrawn);
    }

    @Test
    void savingTheSameDrawAgainChangesNothing() {
        var first = event(0, "First");
        var second = event(1, "Second");
        inTransaction(() -> {
            port().save(gameId, first);
            port().save(gameId, second);
        });

        inTransaction(() -> {
            port().save(gameId, first);
            port().save(gameId, second);
        });

        assertThat(inTransaction(() -> port().findByGameId(gameId))).containsExactly(first, second);
    }

    @Test
    void eventsAreDeletedIndividuallyOrAllAtOnce() {
        var kept = event(0, "Kept");
        var removed = event(1, "Removed");
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
        var event = event(0, "Resolved");
        inTransaction(() -> {
            port().save(gameId, event);
            port().markResolved(gameId, event.eventId());
        });

        inTransaction(() -> port().deleteByGameId(gameId));

        assertThat(inTransaction(() -> port().isResolved(gameId, event.eventId())))
                .isTrue();
    }

    private static GameActiveEvent event(int revealIndex, String title) {
        return new GameActiveEvent(
                UUID.randomUUID(),
                revealIndex,
                title,
                CarryOverState.FRESH,
                List.of(
                        new EventOutcome(UUID.randomUUID(), "Outcome", 60),
                        new EventOutcome(UUID.randomUUID(), "Other", 40)));
    }
}
