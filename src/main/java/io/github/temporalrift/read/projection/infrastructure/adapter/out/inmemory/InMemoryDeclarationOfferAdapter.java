package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.DeclarationOffer;
import io.github.temporalrift.read.projection.domain.port.out.DeclarationOfferRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;

@Repository
@Profile("in-memory")
class InMemoryDeclarationOfferAdapter implements DeclarationOfferRepository {

    private final Table<UUID, DeclarationOffer> offers;

    InMemoryDeclarationOfferAdapter(InMemoryDatabase database) {
        this.offers = database.table("declaration_offer", false);
    }

    @Override
    public Optional<DeclarationOffer> findByGameIdAndPlayerIdAndEraNumber(UUID gameId, UUID playerId, int eraNumber) {
        return offers.find(gameId, RowKey.of(playerId, eraNumber));
    }

    @Override
    public void saveIfAbsent(DeclarationOffer offer) {
        offers.putIfAbsent(offer.gameId(), RowKey.of(offer.playerId(), offer.eraNumber()), offer);
    }

    @Override
    public void deleteByGameIdAndEraNumber(UUID gameId, int eraNumber) {
        offers.removeIf(gameId, offer -> offer.eraNumber() == eraNumber);
    }

    @Override
    public void deleteByGameId(UUID gameId) {
        offers.removePartition(gameId);
    }
}
