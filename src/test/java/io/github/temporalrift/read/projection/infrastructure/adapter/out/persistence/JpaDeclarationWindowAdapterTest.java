package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.read.projection.domain.model.DeclarationWindow;

@ExtendWith(MockitoExtension.class)
class JpaDeclarationWindowAdapterTest {

    @Mock
    DeclarationWindowJpaRepository repository;

    private final UUID gameId = UUID.randomUUID();
    private JpaDeclarationWindowAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaDeclarationWindowAdapter(repository);
    }

    @Test
    void saveIfAbsentAndRead_preservesTheEraDeadline() {
        var window = new DeclarationWindow(gameId, 2, Instant.parse("2030-01-01T10:00:30Z"));
        given(repository.findByGameIdAndEraNumber(gameId, 2)).willReturn(Optional.empty());

        adapter.saveIfAbsent(window);

        var captor = ArgumentCaptor.forClass(DeclarationWindowEntity.class);
        then(repository).should().save(captor.capture());
        assertThat(captor.getValue().toDomain()).isEqualTo(window);

        given(repository.findByGameIdAndEraNumber(gameId, 2)).willReturn(Optional.of(captor.getValue()));
        assertThat(adapter.findByGameIdAndEraNumber(gameId, 2)).contains(window);
    }

    @Test
    void saveIfAbsent_keepsTheFirstRecordedDeadline() {
        var first = new DeclarationWindow(gameId, 2, Instant.parse("2030-01-01T10:00:30Z"));
        given(repository.findByGameIdAndEraNumber(gameId, 2))
                .willReturn(Optional.of(DeclarationWindowEntity.fromDomain(UUID.randomUUID(), first)));

        adapter.saveIfAbsent(new DeclarationWindow(gameId, 2, Instant.parse("2030-01-01T10:00:45Z")));

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
