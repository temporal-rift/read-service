package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.RevealedInfluenceIntel;
import io.github.temporalrift.read.projection.domain.port.out.RevealedInfluenceIntelRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;

@Repository
@Profile("in-memory")
class InMemoryRevealedInfluenceIntelAdapter extends InMemoryRevealedIntelAdapter<RevealedInfluenceIntel>
        implements RevealedInfluenceIntelRepository {

    InMemoryRevealedInfluenceIntelAdapter(InMemoryDatabase database) {
        super(database, "revealed_influence_intel");
    }
}
