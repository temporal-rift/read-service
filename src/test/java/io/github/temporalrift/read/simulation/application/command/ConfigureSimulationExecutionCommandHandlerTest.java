package io.github.temporalrift.read.simulation.application.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.temporalrift.read.shared.GameplayState;
import io.github.temporalrift.read.shared.inmemory.InMemoryDatabase;
import io.github.temporalrift.read.shared.inmemory.InMemoryTransactionManager;
import io.github.temporalrift.read.simulation.application.query.ExecutionCheckpointAssembler;
import io.github.temporalrift.read.simulation.domain.execution.EffectiveRules;
import io.github.temporalrift.read.simulation.domain.execution.EntropyVersion;
import io.github.temporalrift.read.simulation.domain.execution.Execution;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionContext;
import io.github.temporalrift.read.simulation.domain.execution.Seed;
import io.github.temporalrift.read.simulation.domain.execution.SimulationSeat;
import io.github.temporalrift.read.simulation.domain.port.out.EventFence;
import io.github.temporalrift.read.simulation.domain.port.out.ExecutionRepository;

@ExtendWith(MockitoExtension.class)
class ConfigureSimulationExecutionCommandHandlerTest {

    private static final EffectiveRules RULES = new EffectiveRules(Map.of("game.rules.win-score-threshold", 20));
    private static final ExecutionContext CONTEXT = ExecutionContext.of(
            UUID.randomUUID(),
            new Seed("42"),
            EntropyVersion.SHA256_V1,
            "a".repeat(64),
            RULES,
            RULES.digest(),
            Instant.parse("2026-10-05T19:00:00Z"),
            List.of(
                    new SimulationSeat(0, UUID.randomUUID(), "ERASERS"),
                    new SimulationSeat(1, UUID.randomUUID(), "WEAVERS"),
                    new SimulationSeat(2, UUID.randomUUID(), "PROPHETS")));

    @Mock
    ExecutionRepository executions;

    @Mock
    GameplayState gameplay;

    @Mock
    EventFence eventFence;

    @Mock
    ExecutionCheckpointAssembler assembler;

    @Test
    void openingFencesEveryRecordAlreadyInTheSourcesBeforeTheContextExists() {
        given(executions.find()).willReturn(Optional.empty());
        given(executions.findWithLock()).willReturn(Optional.empty());
        given(gameplay.isEmpty()).willReturn(true);
        given(executions.insertIfAbsent(any())).willReturn(true);

        handler().handle(CONTEXT);

        var order = inOrder(eventFence, executions);
        order.verify(eventFence).capture();
        order.verify(executions).insertIfAbsent(Execution.configure(CONTEXT));
    }

    @Test
    void aFailedCaptureOpensNothing() {
        given(executions.find()).willReturn(Optional.empty());
        willThrow(new IllegalStateException("Could not record the event fence"))
                .given(eventFence)
                .capture();

        assertThatThrownBy(() -> handler().handle(CONTEXT)).isInstanceOf(IllegalStateException.class);

        verify(executions, never()).insertIfAbsent(any());
    }

    @Test
    void repeatingTheOpenContextLeavesItsFenceAlone() {
        var open = Execution.configure(CONTEXT);
        given(executions.find()).willReturn(Optional.of(open));
        given(executions.findWithLock()).willReturn(Optional.of(open));

        handler().handle(CONTEXT);

        verify(eventFence, never()).capture();
    }

    private ConfigureSimulationExecutionCommandHandler handler() {
        return new ConfigureSimulationExecutionCommandHandler(
                executions,
                List.of(gameplay),
                eventFence,
                assembler,
                new SimulationControlLock(),
                new TransactionTemplate(new InMemoryTransactionManager(new InMemoryDatabase())));
    }
}
