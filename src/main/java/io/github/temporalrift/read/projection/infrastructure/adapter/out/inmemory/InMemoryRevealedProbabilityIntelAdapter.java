package io.github.temporalrift.read.projection.infrastructure.adapter.out.inmemory;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import io.github.temporalrift.read.projection.domain.model.RevealedProbabilityIntel;
import io.github.temporalrift.read.projection.domain.port.out.RevealedProbabilityIntelRepository;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;

@Repository
@Profile("in-memory")
class InMemoryRevealedProbabilityIntelAdapter extends InMemoryRevealedIntelAdapter<RevealedProbabilityIntel>
        implements RevealedProbabilityIntelRepository {

    InMemoryRevealedProbabilityIntelAdapter(InMemoryDatabase database) {
        super(database, "revealed_probability_intel");
    }
}
