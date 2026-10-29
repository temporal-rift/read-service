package io.github.temporalrift.read.projection.application.query;

import java.util.UUID;

import org.springframework.stereotype.Component;

import io.github.temporalrift.read.projection.domain.model.Phase;
import io.github.temporalrift.read.projection.domain.port.out.GameProjectionRepository;
import io.github.temporalrift.read.shared.GameEndProbe;

/** A game has ended for the read side once its projection applied {@code GameEnded}. */
@Component
class ProjectedGameEndProbe implements GameEndProbe {

    private final GameProjectionRepository gameProjections;

    ProjectedGameEndProbe(GameProjectionRepository gameProjections) {
        this.gameProjections = gameProjections;
    }

    @Override
    public boolean hasEnded(UUID gameId) {
        return gameProjections
                .findByGameId(gameId)
                .map(projection -> projection.phase() == Phase.GAME_ENDED)
                .orElse(false);
    }
}
