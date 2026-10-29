package io.github.temporalrift.read.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.shared.GameplayState;
import io.github.temporalrift.read.shared.ProcessedEventPort;

public abstract class ProcessedEventContract extends PortContract {

    protected abstract ProcessedEventPort port();

    @Test
    void aClaimSucceedsOnceAndARepeatReportsAlreadyClaimed() {
        var eventId = UUID.randomUUID();

        assertThat(inTransaction(() -> port().claim(eventId, "consumer"))).isTrue();
        assertThat(inTransaction(() -> port().claim(eventId, "consumer"))).isFalse();
    }

    @Test
    void aClaimIsScopedToItsConsumerAndItsEvent() {
        var eventId = UUID.randomUUID();
        inTransaction(() -> port().claim(eventId, "consumer"));

        assertThat(inTransaction(() -> port().claim(eventId, "other-consumer"))).isTrue();
        assertThat(inTransaction(() -> port().claim(UUID.randomUUID(), "consumer")))
                .isTrue();
    }

    @Test
    void aClaimOfARolledBackTransactionIsNotRetained() {
        var eventId = UUID.randomUUID();

        assertThatThrownBy(() -> inTransaction(() -> {
                    port().claim(eventId, "consumer");
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(inTransaction(() -> port().claim(eventId, "consumer"))).isTrue();
    }

    @Test
    void discardingEveryClaimLetsAnEventBeClaimedAgain() {
        var claims = (GameplayState) port();
        var eventId = UUID.randomUUID();
        inTransaction(() -> port().claim(eventId, "consumer"));
        assertThat(inTransaction(claims::isEmpty)).isFalse();

        inTransaction(() -> {
            claims.discardAll();
            return null;
        });

        assertThat(inTransaction(claims::isEmpty)).isTrue();
        assertThat(inTransaction(() -> port().claim(eventId, "consumer"))).isTrue();
    }
}
