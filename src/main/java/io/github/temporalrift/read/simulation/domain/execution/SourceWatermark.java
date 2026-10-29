package io.github.temporalrift.read.simulation.domain.execution;

public record SourceWatermark(String groupId, String topic, int partition, long nextOffset) {}
