package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.DeclarationOffer;
import io.github.temporalrift.read.projection.domain.port.out.DeclarationOfferRepository;

@Repository
class JpaDeclarationOfferAdapter implements DeclarationOfferRepository {
    private final DeclarationOfferJpaRepository repository;
    private final ObjectMapper mapper;

    JpaDeclarationOfferAdapter(DeclarationOfferJpaRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<DeclarationOffer> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber) {
        return repository
                .findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, eraNumber)
                .map(entity -> entity.toDomain(mapper));
    }

    @Override
    public void saveIfAbsent(DeclarationOffer offer) {
        if (repository
                .findByGameIdAndPlayerIdAndEraNumber(offer.gameId(), offer.playerId(), offer.eraNumber())
                .isEmpty()) {
            repository.save(DeclarationOfferEntity.fromDomain(UUID.randomUUID(), offer, mapper));
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
