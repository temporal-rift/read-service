package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.JacksonMixin;

import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.SpecialActionAvailability;
import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.SpecialActionUnavailableReason;

/** An available special states its reason as an explicit {@code null}, as the contract describes. */
@JacksonMixin(SpecialActionAvailability.class)
abstract class SpecialActionAvailabilityJacksonMixin {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    abstract SpecialActionUnavailableReason getUnavailableReason();
}
