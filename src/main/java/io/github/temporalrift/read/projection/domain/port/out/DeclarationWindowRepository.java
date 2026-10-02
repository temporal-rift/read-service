package io.github.temporalrift.read.projection.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.DeclarationWindow;

public interface DeclarationWindowRepository {
    Optional<DeclarationWindow> findByGameIdAndEraNumber(UUID gameId, int eraNumber);

    void saveIfAbsent(DeclarationWindow window);

    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    void deleteByGameId(UUID gameId);
}
