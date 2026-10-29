package io.github.temporalrift.read.simulation.infrastructure.adapter.out.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
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
    private static final ExecutionScope UNBOUND = new ExecutionScope(CASE_KEY, null);
    private static final ExecutionScope HOSTING = new ExecutionScope(CASE_KEY, GAME_ID);

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
    void withNoContextOpenNothingIsAdmitted() {
        given(executions.findScope()).willReturn(Optional.empty());

        assertThat(scope().admits(fromKafka(3L))).isFalse();

        verify(executions, never()).bindGame(any(), any());
    }

    @Test
    void theFirstRecordOfAContextBindsItsGameHoldingTheContextExclusively() {
        given(executions.findScope()).willReturn(Optional.of(UNBOUND));
        given(executions.findScopeForBinding()).willReturn(Optional.of(UNBOUND));
        given(eventFence.isFenced("game.events", 0, 3L)).willReturn(false);
        given(executions.isClosedGame(GAME_ID)).willReturn(false);

        assertThat(scope().admits(fromKafka(3L))).isTrue();

        var order = inOrder(executions, eventFence);
        order.verify(executions).findScopeForBinding();
        order.verify(eventFence).isFenced("game.events", 0, 3L);
        order.verify(executions).bindGame(CASE_KEY, GAME_ID);
        verify(executions, never()).findScopeForHandling();
    }

    @Test
    void theFenceIsCheckedOnlyOnceTheContextIsHeld() {
        // The unlocked read can see a context whose open captured a newer fence than any check made before it.
        given(executions.findScope()).willReturn(Optional.of(UNBOUND));
        given(executions.findScopeForBinding()).willReturn(Optional.of(UNBOUND));
        given(eventFence.isFenced("game.events", 0, 3L)).willReturn(true);

        assertThat(scope().admits(fromKafka(3L))).isFalse();

        var order = inOrder(executions, eventFence);
        order.verify(executions).findScopeForBinding();
        order.verify(eventFence).isFenced("game.events", 0, 3L);
        verify(executions, never()).bindGame(any(), any());
    }

    @Test
    void aGameOfAClosedContextDoesNotBindTheOpenOne() {
        given(executions.findScope()).willReturn(Optional.of(UNBOUND));
        given(executions.findScopeForBinding()).willReturn(Optional.of(UNBOUND));
        given(executions.isClosedGame(GAME_ID)).willReturn(true);

        assertThat(scope().admits(inMemory())).isFalse();

        verify(executions, never()).bindGame(any(), any());
    }

    @Test
    void aRecordOfTheHostedGameIsAdmittedUnderASharedLock() {
        given(executions.findScope()).willReturn(Optional.of(HOSTING));
        given(executions.findScopeForHandling()).willReturn(Optional.of(HOSTING));
        given(eventFence.isFenced("game.events", 0, 9L)).willReturn(false);

        assertThat(scope().admits(fromKafka(9L))).isTrue();

        verify(executions, never()).findScopeForBinding();
        verify(executions, never()).bindGame(any(), any());
    }

    @Test
    void anotherGameThanTheHostedOneIsNotAdmitted() {
        var hostingAnother = new ExecutionScope(CASE_KEY, UUID.randomUUID());
        given(executions.findScope()).willReturn(Optional.of(hostingAnother));
        given(executions.findScopeForHandling()).willReturn(Optional.of(hostingAnother));

        assertThat(scope().admits(inMemory())).isFalse();
    }

    @Test
    void aContextClosedBeforeItIsHeldAdmitsNothing() {
        given(executions.findScope()).willReturn(Optional.of(UNBOUND));
        given(executions.findScopeForBinding()).willReturn(Optional.empty());

        assertThat(scope().admits(inMemory())).isFalse();

        verify(executions, never()).bindGame(any(), any());
    }

    @Test
    void aContextReopenedBeforeItIsHeldUnderASharedLockIsNotBound() {
        given(executions.findScope()).willReturn(Optional.of(HOSTING));
        given(executions.findScopeForHandling()).willReturn(Optional.of(UNBOUND));

        assertThat(scope().admits(inMemory())).isFalse();

        verify(executions, never()).bindGame(any(), any());
    }

    private ExecutionInboundScope scope() {
        return new ExecutionInboundScope(executions, eventFence);
    }

    private static InboundRecord fromKafka(long offset) {
        return new InboundRecord("game.events", 0, offset, GAME_ID);
    }

    private static InboundRecord inMemory() {
        return new InboundRecord("game.events", null, null, GAME_ID);
    }
}
