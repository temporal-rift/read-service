package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.ResolutionCardOffer;
import io.github.temporalrift.read.projection.domain.port.out.ResolutionCardOfferRepository;

@Repository
class JpaResolutionCardOfferAdapter implements ResolutionCardOfferRepository {
    private final ResolutionCardOfferJpaRepository repository;
    private final ObjectMapper mapper;

    JpaResolutionCardOfferAdapter(ResolutionCardOfferJpaRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ResolutionCardOffer> findByGameIdAndPlayerIdAndEraNumber(
            UUID gameId, UUID playerId, int eraNumber) {
        return repository
                .findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, eraNumber)
                .map(entity -> entity.toDomain(mapper));
    }

    @Override
    public void upsert(ResolutionCardOffer offer) {
        repository
                .findByGameIdAndPlayerIdAndEraNumber(offer.gameId(), offer.playerId(), offer.eraNumber())
                .ifPresentOrElse(
                        existing -> existing.updateFrom(offer, mapper),
                        () -> repository.save(ResolutionCardOfferEntity.fromDomain(UUID.randomUUID(), offer, mapper)));
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
