package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.ResolutionCardOffer;
import io.github.temporalrift.read.projection.domain.port.out.ResolutionCardOfferRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.RowKey;
import io.github.temporalrift.read.shared.inmemory.Table;

@Repository
@Profile("in-memory")
class InMemoryResolutionCardOfferAdapter implements ResolutionCardOfferRepository {

    private final Table<UUID, ResolutionCardOffer> offers;

    InMemoryResolutionCardOfferAdapter(InMemoryDatabase database) {
        this.offers = database.table("resolution_card_offer", false);
    }

    @Override
    public Optional<ResolutionCardOffer> findByGameIdAndPlayerIdAndEraNumber(
            UUID gameId, UUID playerId, int eraNumber) {
        return offers.find(gameId, RowKey.of(playerId, eraNumber));
    }

    @Override
    public void upsert(ResolutionCardOffer offer) {
        offers.put(offer.gameId(), RowKey.of(offer.playerId(), offer.eraNumber()), offer);
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
