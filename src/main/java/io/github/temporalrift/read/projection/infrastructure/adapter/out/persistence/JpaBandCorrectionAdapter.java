package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.port.out.BandCorrectionRepository;

@Repository
class JpaBandCorrectionAdapter implements BandCorrectionRepository {

    private final BandCorrectionJpaRepository repository;

    JpaBandCorrectionAdapter(BandCorrectionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isCorrectionApplied(UUID gameId, int eraNumber) {
        return repository.existsByGameIdAndEraNumber(gameId, eraNumber);
    }

    @Override
    public void markCorrectionApplied(UUID gameId, int eraNumber) {
        if (!repository.existsByGameIdAndEraNumber(gameId, eraNumber)) {
            repository.save(new BandCorrectionEntity(UUID.randomUUID(), gameId, eraNumber));
        }
    }

    @Override
    public void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        repository.deleteByGameIdAndEraNumber(gameId, eraNumber);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        repository.deleteByGameId(gameId);
    }
}
