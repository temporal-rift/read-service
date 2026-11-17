package io.github.temporalrift.read.projection.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.read.projection.domain.model.GameProjection;
import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;

@ExtendWith(MockitoExtension.class)
class ProjectedGameEndProbeTest {

    @Mock
    GameProjectionRepository gameProjections;

    @Test
    void anUnknownGameHasNotEnded() {
        var gameId = UUID.randomUUID();
        given(gameProjections.findByGameId(gameId)).willReturn(Optional.empty());

        assertThat(probe().hasEnded(gameId)).isFalse();
    }

    @Test
    void aGameInTheEndedPhaseHasEnded() {
        var gameId = UUID.randomUUID();
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 3, Phase.GAME_ENDED)));

        assertThat(probe().hasEnded(gameId)).isTrue();
    }

    @Test
    void aGameInAnyOtherPhaseHasNotEnded() {
        var gameId = UUID.randomUUID();
        given(gameProjections.findByGameId(gameId))
                .willReturn(Optional.of(new GameProjection(gameId, 3, Phase.RESOLUTION)));

        assertThat(probe().hasEnded(gameId)).isFalse();
    }

    private ProjectedGameEndProbe probe() {
        return new ProjectedGameEndProbe(gameProjections);
    }
}
