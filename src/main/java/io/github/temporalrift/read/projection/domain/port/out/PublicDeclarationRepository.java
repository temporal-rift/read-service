package io.github.temporalrift.read.projection.domain.port.out;

import java.util.List;
import java.util.UUID;

import io.github.temporalrift.read.projection.domain.model.PublicDeclaration;

/** Stores game-scoped public declarations, at most one per player per era. */
public interface PublicDeclarationRepository {

    List<PublicDeclaration> findByGameIdAndEraNumber(UUID gameId, int eraNumber);

    void upsert(PublicDeclaration declaration);

    void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber);

    void deleteByGameId(UUID gameId);
}
