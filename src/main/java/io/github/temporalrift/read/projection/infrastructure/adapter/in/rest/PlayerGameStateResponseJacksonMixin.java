package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.JacksonMixin;

import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PlayerGameStateResponse;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.RoundSummary;

@JacksonMixin(PlayerGameStateResponse.class)
abstract class PlayerGameStateResponseJacksonMixin {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    abstract RoundSummary getLastRoundSummary();
}
