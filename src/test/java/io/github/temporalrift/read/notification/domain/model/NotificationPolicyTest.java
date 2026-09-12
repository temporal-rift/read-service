package io.github.temporalrift.read.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class NotificationPolicyTest {

    private final NotificationPolicy policy = new NotificationPolicy();

    @Test
    void classifiesPublicTargetedPrivateAndUnknownEventsExplicitly() {
        assertThat(policy.deliveryFor("ParadoxCascaded")).isEqualTo(NotificationPolicy.Delivery.BROADCAST);
        assertThat(policy.deliveryFor("FactionAssigned")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("HandSelected")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("ProbabilityStateRevealed")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("PlayerJammed")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("InfluenceTraced")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("HandCardIntercepted")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("EraResolutionCompleted")).isEqualTo(NotificationPolicy.Delivery.NEVER);
        assertThat(policy.deliveryFor("ProbabilityStateCalculated")).isEqualTo(NotificationPolicy.Delivery.NEVER);
        assertThat(policy.deliveryFor("UnknownEvent")).isEqualTo(NotificationPolicy.Delivery.NEVER);
        assertThat(policy.deliveryFor("ChainLinkAdded")).isEqualTo(NotificationPolicy.Delivery.BROADCAST);
        assertThat(policy.deliveryFor("ChainCompleted")).isEqualTo(NotificationPolicy.Delivery.BROADCAST);
        assertThat(policy.deliveryFor("ChainBroken")).isEqualTo(NotificationPolicy.Delivery.BROADCAST);
    }

    @Test
    void revealFamilyIsNeverBroadcast() {
        assertThat(policy.deliveryFor("ProbabilityStateRevealed")).isNotEqualTo(NotificationPolicy.Delivery.BROADCAST);
        assertThat(policy.deliveryFor("InfluenceTraced")).isNotEqualTo(NotificationPolicy.Delivery.BROADCAST);
        assertThat(policy.deliveryFor("PlayerJammed")).isNotEqualTo(NotificationPolicy.Delivery.BROADCAST);
        assertThat(policy.deliveryFor("HandCardIntercepted")).isNotEqualTo(NotificationPolicy.Delivery.BROADCAST);
    }

    @Test
    void chainEventsRedactTheirIdentityFieldsOnly() {
        assertThat(policy.identityFieldsToRedact("ChainLinkAdded")).isEqualTo(Set.of("playerId"));
        assertThat(policy.identityFieldsToRedact("ChainCompleted")).isEqualTo(Set.of("playerId"));
        assertThat(policy.identityFieldsToRedact("ChainBroken"))
                .isEqualTo(Set.of("brokenByPlayerId", "targetPlayerId"));
    }

    @Test
    void nonChainEventsHaveNoRedaction() {
        assertThat(policy.identityFieldsToRedact("ParadoxCascaded")).isEmpty();
        assertThat(policy.identityFieldsToRedact("FactionRevealed")).isEmpty();
        assertThat(policy.identityFieldsToRedact("UnknownEvent")).isEmpty();
    }
}
