package io.github.temporalrift.read.simulation.domain.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class EffectiveRulesTest {

    private static final String KEY = "game.rules.value";

    @ParameterizedTest
    @CsvSource({
        "2, 2",
        "2.0, 2",
        "-5, -5",
        "0, 0",
        "-0.0, 0",
        "123.456, 123.456",
        "0.1, 0.1",
        "0.000001, 0.000001",
        "1e-7, 1e-7",
        "1e20, 100000000000000000000",
        "1e21, 1e+21",
        "1.5e300, 1.5e+300",
        "-2.5e-10, -2.5e-10"
    })
    void numbersTakeTheirShortestEcmaScriptForm(String number, String canonical) {
        var rules = new EffectiveRules(Map.of(KEY, new BigDecimal(number)));

        assertThat(rules.canonicalJson()).isEqualTo("{\"" + KEY + "\":" + canonical + "}");
    }

    @Test
    void keysAreOrderedAndStringsListsAndBooleansAreSerialisedCanonically() {
        var properties = new HashMap<String, Object>();
        properties.put("game.rules.b", List.of(1, true, "x"));
        properties.put("game.rules.a", "q\"\\\n\t\b\f\r\u0001");

        var rules = new EffectiveRules(properties);

        assertThat(rules.canonicalJson())
                .isEqualTo("{\"game.rules.a\":\"q\\\"\\\\\\n\\t\\b\\f\\r\\u0001\",\"game.rules.b\":[1,true,\"x\"]}");
    }

    @Test
    void numbersOfTheSameValueAreTheSameRule() {
        assertThat(new EffectiveRules(Map.of(KEY, 2))).isEqualTo(new EffectiveRules(Map.of(KEY, 2.0)));
        assertThat(new EffectiveRules(Map.of(KEY, 2)).digest())
                .isEqualTo(new EffectiveRules(Map.of(KEY, new BigDecimal("2.00"))).digest());
    }

    @Test
    void aKeyOutsideTheGameRulesNamespaceIsRejected() {
        var properties = Map.<String, Object>of("spring.datasource.url", "x");

        assertThatThrownBy(() -> new EffectiveRules(properties))
                .isInstanceOf(InvalidExecutionContextException.class)
                .hasMessageContaining("game.rules.");
    }

    @Test
    void aValueThatIsNotAScalarOrAListOfScalarsIsRejected() {
        var nested = Map.<String, Object>of(KEY, Map.of("a", 1));
        var nestedList = Map.<String, Object>of(KEY, List.of(List.of(1)));
        var missing = new HashMap<String, Object>();
        missing.put(KEY, null);

        assertThatThrownBy(() -> new EffectiveRules(nested)).isInstanceOf(InvalidExecutionContextException.class);
        assertThatThrownBy(() -> new EffectiveRules(nestedList)).isInstanceOf(InvalidExecutionContextException.class);
        assertThatThrownBy(() -> new EffectiveRules(missing)).isInstanceOf(InvalidExecutionContextException.class);
    }

    @Test
    void aNumberWithoutAFiniteDoubleIsRejected() {
        var infinite = Map.<String, Object>of(KEY, Double.POSITIVE_INFINITY);
        var tooLarge = Map.<String, Object>of(KEY, new BigDecimal("1e400"));

        assertThatThrownBy(() -> new EffectiveRules(infinite))
                .isInstanceOf(InvalidExecutionContextException.class)
                .hasMessageContaining("finite");
        assertThatThrownBy(() -> new EffectiveRules(tooLarge))
                .isInstanceOf(InvalidExecutionContextException.class)
                .hasMessageContaining("out of range");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "01", "-1", "18446744073709551616", "1.5"})
    void aSeedMustBeACanonicalUnsigned64BitDecimal(String decimal) {
        assertThatThrownBy(() -> new Seed(decimal)).isInstanceOf(InvalidExecutionContextException.class);
    }

    @Test
    void theLargestUnsigned64BitSeedIsAccepted() {
        assertThat(new Seed("18446744073709551615").decimal()).isEqualTo("18446744073709551615");
    }

    @Test
    void aClockAdvanceNeedsAnOperationATargetAndANonNegativeRevision() {
        var operationId = UUID.randomUUID();
        var target = Instant.parse("2026-10-05T19:00:00Z");

        assertThatThrownBy(() -> new ClockAdvance(null, 0, target)).isInstanceOf(InvalidClockAdvanceException.class);
        assertThatThrownBy(() -> new ClockAdvance(operationId, 0, null))
                .isInstanceOf(InvalidClockAdvanceException.class);
        assertThatThrownBy(() -> new ClockAdvance(operationId, -1, target))
                .isInstanceOf(InvalidClockAdvanceException.class);
        assertThat(new ClockAdvance(operationId, 0, Instant.parse("2026-10-05T19:00:00.123456789Z")).targetTime())
                .isEqualTo(Instant.parse("2026-10-05T19:00:00.123456Z"));
    }
}
