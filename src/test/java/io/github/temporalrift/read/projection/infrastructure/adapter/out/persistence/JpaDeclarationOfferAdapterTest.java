package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import io.github.temporalrift.read.projection.domain.model.DeclarationOffer;

@ExtendWith(MockitoExtension.class)
class JpaDeclarationOfferAdapterTest {

    @Mock
    DeclarationOfferJpaRepository repository;

    private final ObjectMapper mapper = new ObjectMapper();
    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private JpaDeclarationOfferAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaDeclarationOfferAdapter(repository, mapper);
    }

    @Test
    void saveIfAbsentAndRead_preservesTheEligibleModesInOrder() {
        var offer = new DeclarationOffer(
                gameId, playerId, 2, List.of(DeclarationOffer.Mode.MOMENTUM, DeclarationOffer.Mode.RALLY));
        given(repository.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(Optional.empty());

        adapter.saveIfAbsent(offer);

        var captor = ArgumentCaptor.forClass(DeclarationOfferEntity.class);
        then(repository).should().save(captor.capture());
        assertThat(captor.getValue().toDomain(mapper)).isEqualTo(offer);

        given(repository.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(Optional.of(captor.getValue()));
        assertThat(adapter.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .contains(offer);
    }

    @Test
    void saveIfAbsent_keepsTheFirstRecordedOffer() {
        var first = new DeclarationOffer(gameId, playerId, 2, List.of(DeclarationOffer.Mode.RALLY));
        given(repository.findByGameIdAndPlayerIdAndEraNumber(gameId, playerId, 2))
                .willReturn(Optional.of(DeclarationOfferEntity.fromDomain(UUID.randomUUID(), first, mapper)));

        adapter.saveIfAbsent(new DeclarationOffer(gameId, playerId, 2, List.of(DeclarationOffer.Mode.MOMENTUM)));

        then(repository).should(never()).save(any());
    }

    @Test
    void deleteOperations_removeOnlyTheRequestedGameScope() {
        adapter.deleteByGameIdAndEraNumber(gameId, 2);
        adapter.deleteByGameId(gameId);

        then(repository).should().deleteByGameIdAndEraNumber(gameId, 2);
        then(repository).should().deleteByGameId(gameId);
    }
}
