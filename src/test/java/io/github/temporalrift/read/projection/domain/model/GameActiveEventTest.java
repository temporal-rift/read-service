package io.github.temporalrift.read.projection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class GameActiveEventTest {

    private static final UUID EVENT_ID = UUID.randomUUID();

    @Test
    void aCarryOverStateIsRequired() {
        assertThatThrownBy(() -> new GameActiveEvent(EVENT_ID, 0, "Title", null, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("carryOverState must not be null");
    }

    @Test
    void aNegativeRevealIndexIsRejected() {
        assertThatThrownBy(() -> new GameActiveEvent(EVENT_ID, -1, "Title", CarryOverState.FRESH, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("revealIndex must not be negative");
    }

    @Test
    void outcomesAreCopiedSoTheEventCannotSeeLaterMutation() {
        var outcomes = new ArrayList<EventOutcome>();

        var event = new GameActiveEvent(EVENT_ID, 2, "Title", CarryOverState.CASCADED, outcomes);
        outcomes.add(new EventOutcome(UUID.randomUUID(), "description", 50));

        assertThat(event.outcomes()).isEmpty();
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.revealIndex()).isEqualTo(2);
        assertThat(event.title()).isEqualTo("Title");
        assertThat(event.carryOverState()).isEqualTo(CarryOverState.CASCADED);
    }
}
