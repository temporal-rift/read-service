package io.github.temporalrift.read.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationPolicyTest {

    private final NotificationPolicy policy = new NotificationPolicy();

    @Test
    void classifiesPublicTargetedPrivateAndUnknownEventsExplicitly() {
        assertThat(policy.deliveryFor("ParadoxCascaded")).isEqualTo(NotificationPolicy.Delivery.BROADCAST);
        assertThat(policy.deliveryFor("FactionAssigned")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("HandSelected")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("EraResolutionCompleted")).isEqualTo(NotificationPolicy.Delivery.NEVER);
        assertThat(policy.deliveryFor("ProbabilityStateCalculated")).isEqualTo(NotificationPolicy.Delivery.NEVER);
        assertThat(policy.deliveryFor("UnknownEvent")).isEqualTo(NotificationPolicy.Delivery.NEVER);
    }
}
