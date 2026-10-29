package io.github.temporalrift.read.simulation.infrastructure.adapter.out.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.temporalrift.read.shared.InboundRecord;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionScope;
import io.github.temporalrift.read.simulation.domain.port.out.EventFence;
import io.github.temporalrift.read.simulation.domain.port.out.ExecutionRepository;

@ExtendWith(MockitoExtension.class)
class ExecutionInboundScopeTest {

    private static final UUID CASE_KEY = UUID.randomUUID();
    private static final UUID GAME_ID = UUID.randomUUID();

    @Mock
    ExecutionRepository executions;

    @Mock
    EventFence eventFence;

    @Test
    void aRecordWithoutAGameIsNeverAdmitted() {
        assertThat(scope().admits(new InboundRecord("game.events", 0, 3L, null)))
                .isFalse();

        verifyNoInteractions(executions, eventFence);
    }

    @Test
    void aRecordBelowTheCloseFenceIsNotAdmittedWhateverItsGame() {
        given(eventFence.isFenced("game.events", 1, 3L)).willReturn(true);

        assertThat(scope().admits(new InboundRecord("game.events", 1, 3L, GAME_ID)))
                .isFalse();

        verifyNoInteractions(executions);
    }

    @Test
    void withNoContextOpenNothingIsAdmitted() {
        given(executions.findScope()).willReturn(Optional.empty());

        assertThat(scope().admits(inMemory(GAME_ID))).isFalse();

        verify(executions, never()).bindGame(any(), any());
    }

    @Test
    void theFirstRecordOfAContextBindsItsGame() {
        given(executions.findScope()).willReturn(Optional.of(new ExecutionScope(CASE_KEY, null)));
        given(executions.isClosedGame(GAME_ID)).willReturn(false);
        given(executions.findScopeForHandling()).willReturn(Optional.of(new ExecutionScope(CASE_KEY, GAME_ID)));

        assertThat(scope().admits(inMemory(GAME_ID))).isTrue();

        verify(executions).bindGame(CASE_KEY, GAME_ID);
    }

    @Test
    void aGameOfAClosedContextDoesNotBindTheOpenOne() {
        given(executions.findScope()).willReturn(Optional.of(new ExecutionScope(CASE_KEY, null)));
        given(executions.isClosedGame(GAME_ID)).willReturn(true);
        given(executions.findScopeForHandling()).willReturn(Optional.of(new ExecutionScope(CASE_KEY, null)));

        assertThat(scope().admits(inMemory(GAME_ID))).isFalse();

        verify(executions, never()).bindGame(any(), any());
    }

    @Test
    void anotherGameThanTheHostedOneIsNotAdmitted() {
        var hosted = new ExecutionScope(CASE_KEY, UUID.randomUUID());
        given(executions.findScope()).willReturn(Optional.of(hosted));
        given(executions.findScopeForHandling()).willReturn(Optional.of(hosted));

        assertThat(scope().admits(inMemory(GAME_ID))).isFalse();

        verify(executions, never()).bindGame(any(), any());
    }

    @Test
    void aContextClosedWhileBindingAdmitsNothing() {
        given(executions.findScope()).willReturn(Optional.of(new ExecutionScope(CASE_KEY, null)));
        given(executions.isClosedGame(GAME_ID)).willReturn(false);
        given(executions.findScopeForHandling()).willReturn(Optional.empty());

        assertThat(scope().admits(inMemory(GAME_ID))).isFalse();
    }

    @Test
    void aRecordAboveTheFenceOfTheHostedGameIsAdmitted() {
        given(eventFence.isFenced("timeline.events", 0, 9L)).willReturn(false);
        var hosted = new ExecutionScope(CASE_KEY, GAME_ID);
        given(executions.findScope()).willReturn(Optional.of(hosted));
        given(executions.findScopeForHandling()).willReturn(Optional.of(hosted));

        assertThat(scope().admits(new InboundRecord("timeline.events", 0, 9L, GAME_ID)))
                .isTrue();
    }

    private ExecutionInboundScope scope() {
        return new ExecutionInboundScope(executions, eventFence);
    }

    private static InboundRecord inMemory(UUID gameId) {
        return new InboundRecord("game.events", null, null, gameId);
    }
}
