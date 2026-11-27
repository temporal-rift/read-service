package io.github.temporalrift.read.projection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SpecialAvailabilityReportTest {

    private static final UUID GAME_ID = UUID.randomUUID();
    private static final UUID PLAYER_ID = UUID.randomUUID();

    @Test
    void specialsAreCopiedSoTheReportCannotSeeLaterMutation() {
        var specials = new ArrayList<>(List.of(new SpecialAvailability("SCAN", true, null)));

        var report = new SpecialAvailabilityReport(GAME_ID, PLAYER_ID, 1, 2, specials);
        specials.add(new SpecialAvailability("JAM", false, "used"));

        assertThat(report.specials()).hasSize(1);
    }

    @Test
    void anEarlierEraIsOlder() {
        var report = new SpecialAvailabilityReport(GAME_ID, PLAYER_ID, 1, 3, List.of());
        var other = new SpecialAvailabilityReport(GAME_ID, PLAYER_ID, 2, 1, List.of());

        assertThat(report.isOlderThan(other)).isTrue();
        assertThat(other.isOlderThan(report)).isFalse();
    }

    @Test
    void anEarlierRoundOfTheSameEraIsOlder() {
        var report = new SpecialAvailabilityReport(GAME_ID, PLAYER_ID, 1, 2, List.of());
        var other = new SpecialAvailabilityReport(GAME_ID, PLAYER_ID, 1, 3, List.of());

        assertThat(report.isOlderThan(other)).isTrue();
        assertThat(other.isOlderThan(report)).isFalse();
        assertThat(report.isOlderThan(report)).isFalse();
    }

    @Test
    void isForMatchesTheExactEraAndRound() {
        var report = new SpecialAvailabilityReport(GAME_ID, PLAYER_ID, 2, 3, List.of());

        assertThat(report.isFor(2, 3)).isTrue();
        assertThat(report.isFor(2, 2)).isFalse();
        assertThat(report.isFor(1, 3)).isFalse();
    }
}
