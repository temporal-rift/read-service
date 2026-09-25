package io.github.temporalrift.read.projection.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.JacksonMixin;

import io.github.temporalrift.read.projection.infrastructure.adapter.in.rest.v1.model.PlayerGameStateResponseMyForesightPreview;

@JacksonMixin(PlayerGameStateResponseMyForesightPreview.class)
abstract class ForesightPreviewResponseJacksonMixin {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    abstract String getEmptyReason();
}
