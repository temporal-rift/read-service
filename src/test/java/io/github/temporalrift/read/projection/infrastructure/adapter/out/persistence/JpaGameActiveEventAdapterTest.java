package io.github.temporalrift.read.projection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.read.projection.domain.model.CarryOverState;
import io.github.temporalrift.read.projection.domain.model.EventOutcome;
import io.github.temporalrift.read.projection.domain.model.GameActiveEvent;

@ExtendWith(MockitoExtension.class)
class JpaGameActiveEventAdapterTest {

    @Mock
    GameActiveEventJpaRepository repository;

    @Mock
    GameResolvedEventJpaRepository resolvedRepository;

    private final UUID gameId = UUID.randomUUID();
    private JpaGameActiveEventAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaGameActiveEventAdapter(repository, resolvedRepository);
    }

    @Test
    void save_persistsEachOutcomeWithItsPrintedStartingWeight() {
        var event = new GameActiveEvent(
                UUID.randomUUID(),
                "The Plague Ship in Harbor",
                CarryOverState.CASCADED,
                List.of(
                        new EventOutcome(UUID.randomUUID(), "Contained", 60),
                        new EventOutcome(UUID.randomUUID(), "Spreads", 25),
                        new EventOutcome(UUID.randomUUID(), "Sunk", 15)));
        given(repository.findByGameIdAndEventId(gameId, event.eventId())).willReturn(Optional.empty());

        adapter.save(gameId, event);

        var captor = ArgumentCaptor.forClass(GameActiveEventEntity.class);
        then(repository).should().save(captor.capture());
        assertThat(captor.getValue().toDomain()).isEqualTo(event);
    }
}
