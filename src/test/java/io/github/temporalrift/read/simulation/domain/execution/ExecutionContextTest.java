package io.github.temporalrift.read.simulation.domain.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ExecutionContextTest {

    // The contract's example rules and digest.
    private static final Map<String, Object> RULES =
            Map.of("game.rules.probability.amplify-multiplier.II", 2, "game.rules.win-score-threshold", 20);
    private static final String RULES_DIGEST = "2f4e77d6f2f5ad41eb3b05de091e524133995ef6f274a58f539adbf2d5b06fad";
    private static final String MANIFEST_DIGEST = "a".repeat(64);
    private static final Instant LOGICAL_TIME = Instant.parse("2026-10-05T19:00:00.123456789Z");

    @Test
    void theRulesDigestIsTheirCanonicalSha256() {
        assertThat(new EffectiveRules(RULES).digest()).isEqualTo(RULES_DIGEST);
        assertThat(new EffectiveRules(Map.of(
                                "game.rules.probability.amplify-multiplier.II",
                                new BigDecimal("2.0"),
                                "game.rules.win-score-threshold",
                                20L))
                        .digest())
                .isEqualTo(RULES_DIGEST);
    }

    @Test
    void aContextWhoseDigestIsNotItsRulesIsRejected() {
        var rules = new EffectiveRules(Map.of("game.rules.win-score-threshold", 21));

        assertThatThrownBy(() -> context(rules, RULES_DIGEST))
                .isInstanceOf(InvalidExecutionContextException.class)
                .hasMessageContaining("rulesDigest");
    }

    @Test
    void aRuleOutsideTheGameRulesNamespaceIsRejected() {
        assertThatThrownBy(() -> new EffectiveRules(Map.of("spring.datasource.url", "x")))
                .isInstanceOf(InvalidExecutionContextException.class)
                .hasMessageContaining("game.rules.");
    }

    @Test
    void readServiceKeepsTheRulesOnlyByTheirDigestAndTimeAtMicroseconds() {
        var context = context(new EffectiveRules(RULES), RULES_DIGEST);

        assertThat(context.rulesDigest()).isEqualTo(RULES_DIGEST);
        assertThat(context.logicalTime()).isEqualTo(Instant.parse("2026-10-05T19:00:00.123456Z"));
    }

    @Test
    void seatsMustBeContiguousWithUniquePlayersAndFactions() {
        var player = UUID.randomUUID();

        assertThatThrownBy(() -> context(List.of(
                        new SimulationSeat(0, UUID.randomUUID(), "ERASERS"),
                        new SimulationSeat(2, UUID.randomUUID(), "WEAVERS"),
                        new SimulationSeat(3, UUID.randomUUID(), "PROPHETS"))))
                .isInstanceOf(InvalidExecutionContextException.class);
        assertThatThrownBy(() -> context(List.of(
                        new SimulationSeat(0, player, "ERASERS"),
                        new SimulationSeat(1, player, "WEAVERS"),
                        new SimulationSeat(2, UUID.randomUUID(), "PROPHETS"))))
                .isInstanceOf(InvalidExecutionContextException.class);
        assertThatThrownBy(() -> context(List.of(
                        new SimulationSeat(0, UUID.randomUUID(), "ERASERS"),
                        new SimulationSeat(1, UUID.randomUUID(), "ERASERS"),
                        new SimulationSeat(2, UUID.randomUUID(), "PROPHETS"))))
                .isInstanceOf(InvalidExecutionContextException.class);
        assertThatThrownBy(() -> context(List.of(
                        new SimulationSeat(0, UUID.randomUUID(), "ERASERS"),
                        new SimulationSeat(1, UUID.randomUUID(), "WEAVERS"))))
                .isInstanceOf(InvalidExecutionContextException.class);
    }

    private static ExecutionContext context(EffectiveRules rules, String rulesDigest) {
        return ExecutionContext.of(
                UUID.randomUUID(),
                new Seed("42"),
                EntropyVersion.SHA256_V1,
                MANIFEST_DIGEST,
                rules,
                rulesDigest,
                LOGICAL_TIME,
                seats());
    }

    private static ExecutionContext context(List<SimulationSeat> seats) {
        return ExecutionContext.of(
                UUID.randomUUID(),
                new Seed("42"),
                EntropyVersion.SHA256_V1,
                MANIFEST_DIGEST,
                new EffectiveRules(RULES),
                RULES_DIGEST,
                LOGICAL_TIME,
                seats);
    }

    private static List<SimulationSeat> seats() {
        return List.of(
                new SimulationSeat(0, UUID.randomUUID(), "ERASERS"),
                new SimulationSeat(1, UUID.randomUUID(), "WEAVERS"),
                new SimulationSeat(2, UUID.randomUUID(), "PROPHETS"));
    }
}
