package io.github.temporalrift.read.projection.application.query;

import java.util.Comparator;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.temporalrift.read.projection.application.port.in.GetGameHistoryUseCase;
import io.github.temporalrift.read.projection.domain.model.GameHistoryNotFoundException;
import io.github.temporalrift.read.projection.domain.port.out.GameHistoryRepository;

@Service
class GetGameHistoryQueryHandler implements GetGameHistoryUseCase {

    private final GameHistoryRepository histories;

    GetGameHistoryQueryHandler(GameHistoryRepository histories) {
        this.histories = histories;
    }

    @Override
    @Transactional(readOnly = true)
    public Result get(UUID gameId, UUID playerId) {
        var eras = histories.findByGameId(gameId).stream()
                .sorted(Comparator.comparingInt(history -> history.eraNumber()))
                .map(history -> new EraResult(
                        history.eraNumber(),
                        history.resolvedOutcomes(),
                        history.paradoxesCascaded(),
                        history.cascadedEvents(),
                        history.myHand(playerId)))
                .toList();
        if (eras.isEmpty()) {
            throw new GameHistoryNotFoundException(gameId);
        }
        return new Result(gameId, eras);
    }
}
