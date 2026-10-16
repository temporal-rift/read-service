package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.RevealedHandCardIntel;
import io.github.temporalrift.read.projection.domain.port.out.RevealedHandCardIntelRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;

@Repository
@Profile("in-memory")
class InMemoryRevealedHandCardIntelAdapter extends InMemoryRevealedIntelAdapter<RevealedHandCardIntel>
        implements RevealedHandCardIntelRepository {

    InMemoryRevealedHandCardIntelAdapter(InMemoryDatabase database) {
        super(database, "revealed_hand_card_intel");
    }
}
