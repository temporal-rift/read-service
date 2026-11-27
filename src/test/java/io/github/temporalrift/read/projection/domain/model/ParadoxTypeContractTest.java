package io.github.temporalrift.read.projection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.temporalrift.asyncapi.timelineevents.GeneratedChannelContract;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.ParadoxType;

/**
 * The projection's paradox types are mapped by name from the timeline event contract and to the REST contract, so the
 * three enums must hold exactly the same values.
 */
class ParadoxTypeContractTest {

    /** {@code UNKNOWN} is the decoder's fallback for an unrecognized wire value, not a published paradox type. */
    @ParameterizedTest
    @EnumSource(value = GeneratedChannelContract.ParadoxType.class, mode = EnumSource.Mode.EXCLUDE, names = "UNKNOWN")
    void everyPublishedParadoxTypeIsProjected(GeneratedChannelContract.ParadoxType published) {
        assertThat(ParadoxType.valueOf(published.name()).name()).isEqualTo(published.name());
    }

    @ParameterizedTest
    @EnumSource(ParadoxType.class)
    void everyProjectedParadoxTypeIsServed(ParadoxType projected) {
        assertThat(ParadoxType.fromValue(projected.name()).getValue()).isEqualTo(projected.name());
    }

    @Test
    void fateContradictionIsAmongTheProjectedTypes() {
        assertThat(Arrays.stream(ParadoxType.values()).map(Enum::name)).contains("FATE_CONTRADICTION");
    }
}
