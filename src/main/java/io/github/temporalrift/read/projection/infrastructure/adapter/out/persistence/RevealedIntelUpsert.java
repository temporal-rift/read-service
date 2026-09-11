package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.temporalrift.read.projection.domain.model.RevealedIntelEntry;

/** Latest-round-wins upsert over the per-kind revealed-intel identity. */
final class RevealedIntelUpsert {

    private RevealedIntelUpsert() {}

    static <D extends RevealedIntelEntry, E extends RevealedIntelBaseEntity> void upsertLatest(
            D intel, Function<D, Optional<E>> finder, BiConsumer<E, D> updater, Runnable inserter) {
        finder.apply(intel)
                .ifPresentOrElse(
                        existing -> {
                            if (intel.observedInRound() >= existing.getObservedInRound()) {
                                updater.accept(existing, intel);
                            }
                        },
                        inserter);
    }
}
