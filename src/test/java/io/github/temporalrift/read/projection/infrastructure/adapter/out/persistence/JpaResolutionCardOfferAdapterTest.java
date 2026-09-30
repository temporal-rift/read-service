package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import io.github.temporalrift.read.projection.domain.model.ResolutionCardOffer;

@ExtendWith(MockitoExtension.class)
class JpaResolutionCardOfferAdapterTest {

    @Mock
    ResolutionCardOfferJpaRepository repository;

    private final ObjectMapper mapper = new ObjectMapper();
    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private JpaResolutionCardOfferAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaResolutionCardOfferAdapter(repository, mapper);
    }

    @Test
    void upsertAndRead_preservesThePlayersCompleteCardOffer() {
        var card = new ResolutionCardOffer.Card(UUID.randomUUID(), "STABILIZE", "I");
        var offer = new ResolutionCardOffer(gameId, playerId, 2, List.of(card));
        given(repository.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(Optional.empty());

        adapter.upsert(offer);

        var captor = ArgumentCaptor.forClass(ResolutionCardOfferEntity.class);
        then(repository).should().save(captor.capture());
        assertThat(captor.getValue().toDomain(mapper)).isEqualTo(offer);

        given(repository.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(Optional.of(captor.getValue()));
        assertThat(adapter.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .contains(offer);
    }

    @Test
    void upsertEmptyOffer_replacesTheExistingOffer() {
        var original = new ResolutionCardOffer(
                gameId, playerId, 2, List.of(new ResolutionCardOffer.Card(UUID.randomUUID(), "DETONATE", "I")));
        var empty = new ResolutionCardOffer(gameId, playerId, 2, List.of());
        var existing = ResolutionCardOfferEntity.fromDomain(UUID.randomUUID(), original, mapper);
        given(repository.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(Optional.of(existing));

        adapter.upsert(empty);

        assertThat(existing.toDomain(mapper).cards()).isEmpty();
        then(repository).should(never()).save(existing);
    }

    @Test
    void deleteOperations_removeOnlyTheRequestedGameScope() {
        adapter.deleteByGameIdAndEraNumber(gameId, 2);
        adapter.deleteByGameId(gameId);

        then(repository).should().deleteByGameIdAndEraNumber(gameId, 2);
        then(repository).should().deleteByGameId(gameId);
    }
}
