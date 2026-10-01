package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.github.temporalrift.read.projection.domain.model.GameProjectionNotReadyException;

class ProjectionExceptionHandlerTest {

    @Test
    void mapsUnavailableGameProjectionToServiceUnavailable() {
        var response =
                new ProjectionExceptionHandler().handleGameProjectionNotReady(new GameProjectionNotReadyException());

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getDetail()).isEqualTo("Game state is not ready. Retry shortly.");
    }
}
