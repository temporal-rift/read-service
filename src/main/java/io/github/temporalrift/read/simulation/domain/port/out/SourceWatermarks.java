package io.github.temporalrift.read.simulation.domain.port.out;

import java.util.List;

import io.github.temporalrift.read.simulation.domain.execution.SourceWatermark;

public interface SourceWatermarks {

    List<SourceWatermark> current();
}
