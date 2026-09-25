package io.github.temporalrift.read.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class NotificationPolicyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final NotificationPolicy policy = new NotificationPolicy();

    @Test
    void classifiesPublicTargetedPrivateAndUnknownEventsExplicitly() {
        assertThat(policy.deliveryFor("ParadoxCascaded")).isEqualTo(NotificationPolicy.Delivery.BROADCAST);
        assertThat(policy.deliveryFor("BandedProbabilityPublished")).isEqualTo(NotificationPolicy.Delivery.BROADCAST);
        assertThat(policy.deliveryFor("AdjustedBandsPublished")).isEqualTo(NotificationPolicy.Delivery.BROADCAST);
        assertThat(policy.deliveryFor("FactionAssigned")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("HandSelected")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("ProbabilityStateRevealed")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("PlayerJammed")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("InfluenceTraced")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("HandCardIntercepted")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("ForesightRevealed")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("ThreadRejected")).isEqualTo(NotificationPolicy.Delivery.TARGETED);
        assertThat(policy.deliveryFor("ChainLinkInvalidated")).isEqualTo(NotificationPolicy.Delivery.BROADCAST);
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
        assertThat(policy.deliveryFor("ForesightRevealed")).isNotEqualTo(NotificationPolicy.Delivery.BROADCAST);
        assertThat(policy.deliveryFor("ThreadRejected")).isNotEqualTo(NotificationPolicy.Delivery.BROADCAST);
    }

    @Test
    void chainEventsRedactTheirIdentityFieldsOnly() {
        assertThat(policy.identityFieldsToRedact("ChainLinkAdded")).isEqualTo(Set.of("playerId"));
        assertThat(policy.identityFieldsToRedact("ChainCompleted")).isEqualTo(Set.of("playerId"));
        assertThat(policy.identityFieldsToRedact("ChainBroken"))
                .isEqualTo(Set.of("playerId", "brokenByPlayerId", "targetPlayerId"));
    }

    @Test
    void nonChainEventsHaveNoRedaction() {
        assertThat(policy.identityFieldsToRedact("ParadoxCascaded")).isEmpty();
        assertThat(policy.identityFieldsToRedact("FactionRevealed")).isEmpty();
        assertThat(policy.identityFieldsToRedact("UnknownEvent")).isEmpty();
    }

    @Test
    void publicCascadePayloadOmitsCarryForwardWeightsWithoutMutatingTheSource() {
        var payload = objectMapper.readTree("""
                {"affectedEventId":"%s","carryForwardProbabilityState":[
                  {"outcomeId":"%s","probability":50}
                ]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));

        var publicPayload = policy.publicPayloadFor("ParadoxCascaded", payload);

        assertThat(publicPayload.has("carryForwardProbabilityState")).isFalse();
        assertThat(payload.has("carryForwardProbabilityState")).isTrue();
    }

    @Test
    void publicEventDrawOmitsWeightsOnlyForCarriedEvents() {
        var payload = objectMapper.readTree("""
                {"events":[
                  {"carryOverState":"CASCADED","outcomes":[{"initialProbability":50,"probability":50}]},
                  {"carryOverState":"FRESH","outcomes":[{"initialProbability":34,"probability":34}]}
                ]}
                """);

        var publicPayload = policy.publicPayloadFor("EventsDrawn", payload);

        assertThat(publicPayload.get("events").get(0).get("outcomes").get(0).has("initialProbability"))
                .isFalse();
        assertThat(publicPayload.get("events").get(0).get("outcomes").get(0).has("probability"))
                .isFalse();
        assertThat(publicPayload
                        .get("events")
                        .get(1)
                        .get("outcomes")
                        .get(0)
                        .get("initialProbability")
                        .asInt())
                .isEqualTo(34);
    }

    @Test
    void privateProbabilityRevealRetainsExactWeights() {
        var payload = objectMapper.readTree("""
                {"playerId":"%s","outcomes":[{"outcomeId":"%s","probability":50}]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));

        var privatePayload = policy.publicPayloadFor("ProbabilityStateRevealed", payload);

        assertThat(privatePayload.get("outcomes").get(0).get("probability").asInt())
                .isEqualTo(50);
    }

    @Test
    void scoresUpdatedKeepsFactionAndReasonOnlyForTheOwningEntry() {
        var owner = UUID.randomUUID();
        var opponent = UUID.randomUUID();
        var payload = objectMapper.readTree("""
                {"gameId":"%s","eraNumber":1,"updates":[
                    {"playerId":"%s","faction":"PROPHETS","pointsDelta":4,
                     "reason":"EVENT_RESOLVED_AS_WRITTEN","newTotal":12},
                    {"playerId":"%s","faction":"ERASERS","pointsDelta":2,
                     "reason":"ANNIHILATED_OUTCOME","newTotal":6}
                ]}
                """.formatted(UUID.randomUUID(), owner, opponent));

        var view = policy.scoresUpdatedFor(payload, owner);

        var updates = view.get("updates");
        var ownEntry = updates.get(0);
        assertThat(ownEntry.get("faction").asText()).isEqualTo("PROPHETS");
        assertThat(ownEntry.get("reason").asText()).isEqualTo("EVENT_RESOLVED_AS_WRITTEN");
        assertThat(ownEntry.get("playerId").asText()).isEqualTo(owner.toString());
        assertThat(ownEntry.get("pointsDelta").asInt()).isEqualTo(4);
        assertThat(ownEntry.get("newTotal").asInt()).isEqualTo(12);

        var opponentEntry = updates.get(1);
        assertThat(opponentEntry.has("faction")).isFalse();
        assertThat(opponentEntry.has("reason")).isFalse();
        assertThat(opponentEntry.get("playerId").asText()).isEqualTo(opponent.toString());
        assertThat(opponentEntry.get("pointsDelta").asInt()).isEqualTo(2);
        assertThat(opponentEntry.get("newTotal").asInt()).isEqualTo(6);
    }

    @Test
    void scoresUpdatedStripsEveryEntryWhenViewerOwnsNone() {
        var payload = objectMapper.readTree("""
                {"gameId":"%s","eraNumber":0,"updates":[
                    {"playerId":"%s","faction":"REVISIONISTS","pointsDelta":6,
                     "reason":"FACTION_UNIDENTIFIED","newTotal":18}
                ]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));

        var view = policy.scoresUpdatedFor(payload, UUID.randomUUID());

        var entry = view.get("updates").get(0);
        assertThat(entry.has("faction")).isFalse();
        assertThat(entry.has("reason")).isFalse();
    }

    @Test
    void scoresUpdatedPassesThroughPayloadsWithoutAnUpdatesArray() {
        var payload = objectMapper.readTree("{\"gameId\":\"" + UUID.randomUUID() + "\"}");

        assertThat(policy.scoresUpdatedFor(payload, UUID.randomUUID())).isEqualTo(payload);
    }
}
