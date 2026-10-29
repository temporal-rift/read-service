package io.github.temporalrift.read.simulation.infrastructure.adapter.in.rest;

import java.time.ZoneOffset;

import io.github.temporalrift.read.simulation.domain.execution.ClockAcknowledgement;
import io.github.temporalrift.read.simulation.domain.execution.ClockAdvance;
import io.github.temporalrift.read.simulation.domain.execution.EffectiveRules;
import io.github.temporalrift.read.simulation.domain.execution.EntropyVersion;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionCheckpoint;
import io.github.temporalrift.read.simulation.domain.execution.ExecutionContext;
import io.github.temporalrift.read.simulation.domain.execution.Seed;
import io.github.temporalrift.read.simulation.domain.execution.SimulationSeat;
import io.github.temporalrift.read.simulation.infrastructure.adapter.in.rest.v1.model.ExecutionState;
import io.github.temporalrift.read.simulation.infrastructure.adapter.in.rest.v1.model.SourceWatermark;

final class SimulationControlMapper {

    private SimulationControlMapper() {}

    static ExecutionContext toDomain(
            io.github.temporalrift.read.simulation.infrastructure.adapter.in.rest.v1.model.ExecutionContext body) {
        return ExecutionContext.of(
                body.getCaseKey(),
                new Seed(body.getSeed()),
                EntropyVersion.valueOf(body.getEntropyVersion().getValue()),
                body.getManifestDigest(),
                new EffectiveRules(body.getRules().getProperties()),
                body.getRulesDigest(),
                body.getLogicalTime().toInstant(),
                body.getSeats().stream()
                        .map(seat -> new SimulationSeat(
                                seat.getSeatIndex(),
                                seat.getPlayerId(),
                                seat.getFaction().getValue()))
                        .toList());
    }

    static ClockAdvance toDomain(
            io.github.temporalrift.read.simulation.infrastructure.adapter.in.rest.v1.model.ClockAdvance body) {
        return new ClockAdvance(
                body.getOperationId(),
                body.getExpectedRevision(),
                body.getTargetTime().toInstant());
    }

    static io.github.temporalrift.read.simulation.infrastructure.adapter.in.rest.v1.model.ExecutionCheckpoint toModel(
            ExecutionCheckpoint checkpoint) {
        return new io.github.temporalrift.read.simulation.infrastructure.adapter.in.rest.v1.model.ExecutionCheckpoint(
                checkpoint.caseKey(),
                checkpoint.manifestDigest(),
                checkpoint.rulesDigest(),
                checkpoint.revision(),
                checkpoint.logicalTime().atOffset(ZoneOffset.UTC),
                checkpoint.gameId(),
                ExecutionState.fromValue(checkpoint.state().name()),
                checkpoint.drained(),
                checkpoint.outboxPending(),
                checkpoint.continuationsPending(),
                checkpoint.dueTimersPending(),
                checkpoint.nextDeadline() == null
                        ? null
                        : checkpoint.nextDeadline().atOffset(ZoneOffset.UTC),
                checkpoint.sourceWatermarks().stream()
                        .map(watermark -> new SourceWatermark(
                                watermark.groupId(), watermark.topic(), watermark.partition(), watermark.nextOffset()))
                        .toList());
    }

    static io.github.temporalrift.read.simulation.infrastructure.adapter.in.rest.v1.model.ClockAcknowledgement toModel(
            ClockAcknowledgement acknowledgement) {
        return new io.github.temporalrift.read.simulation.infrastructure.adapter.in.rest.v1.model.ClockAcknowledgement(
                acknowledgement.operationId(),
                acknowledgement.appliedRevision(),
                acknowledgement.logicalTime().atOffset(ZoneOffset.UTC));
    }
}
